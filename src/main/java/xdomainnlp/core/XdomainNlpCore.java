package xdomainnlp.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.rdf4j.model.Model;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import xdomainnlp.api.NifContext;
import xdomainnlp.api.NoveltyParameter;
import xdomainnlp.api.Post;
import xdomainnlp.db.TriplestoreManagerImpl;
import xdomainnlp.entitylinking.AgdistisCore;
import xdomainnlp.namedentityrecognition.AnnotationMerger;
import xdomainnlp.noveltydetection.NoveltyDetector;
import xdomainnlp.noveltydetection.TopicModeler;

public class XdomainNlpCore {

	/**
	 * Setup and run the whole pipeline.
	 * 
	 * @param inputDoc
	 *            Document to be annotated.
	 * @param noveltyParam
	 *            Parameter object for the novelty detection.
	 * @param reqAnnotator
	 *            Annotator that is applied on the input text.
	 * @param serviceBaseUri
	 *            Base URI of the service.
	 * @param tripleStoreManager
	 *            Triplestoremanager object.
	 * @throws InterruptedException
	 *             Interrupted Exception
	 * @throws IOException
	 *             IO Exception
	 * @throws ParseException
	 *             Parse Exception
	 */
	public void runPipeline(Post inputDoc, NoveltyParameter noveltyParam, String reqAnnotator, String serviceBaseUri,
			TriplestoreManagerImpl tripleStoreManager) throws InterruptedException, IOException, ParseException {
		String domain = inputDoc.getDomain();
		File nerOntologyIndexDir = new File("src/main/resources/ontology-index/" + domain);
		File namedEntityTypesPropsFile = new File(
				"src/main/resources/ontology/" + domain + "/" + AnnotationMerger.PROPERTIES_FILE_NAMED_ENTITY_TYPES + ".properties");

		/*
		 * CoreNLP
		 */
		String inputDocText = inputDoc.getBody();
		Properties coreNlpProp = loadNlpProperties(domain, reqAnnotator);
		StanfordCoreNLP coreNlpPipeline = new StanfordCoreNLP(coreNlpProp);
		Annotation coreNlpDoc = new Annotation(inputDocText);
		coreNlpPipeline.annotate(coreNlpDoc);

		/*
		 * Named Entity Recognition and optional Entity Linking
		 */
		if (reqAnnotator.equals("ner")) {
			AnnotationMerger am = new AnnotationMerger(domain, inputDocText, coreNlpDoc);
			if (nerOntologyIndexDir.isDirectory() && nerOntologyIndexDir.list().length > 0) {
				am.mergeAnnotations();
			} else if (namedEntityTypesPropsFile.exists()) {
				am.setCoreNlpTypes();
			}
		}

		AgdistisCore agdistis = new AgdistisCore(coreNlpDoc, inputDocText);
		agdistis.disambiguateEntities();

		/*
		 * Novelty Detection
		 */
		if (noveltyParam.isNoveltyComputed()) {
			TopicModeler.inferTopicDistribution(inputDoc);
			NoveltyDetector noveltyDetector = new NoveltyDetector(inputDoc, 100);
			noveltyDetector.computeMaxNovelty(noveltyParam.getWeight(), noveltyParam.getThreshold());
		}

		/*
		 * Semantic Representation
		 */
		NifCore nifCore = new NifCore(inputDoc, coreNlpDoc, noveltyParam, agdistis, reqAnnotator, serviceBaseUri, tripleStoreManager);
		NifContext nifContext = nifCore.buildContext();
		Model rdfModel = nifContext.createRDFModel();
		nifContext.saveRdfModelInTripleStore(rdfModel);
	}

	/**
	 * Set all necessary properties for the NLP pipeline.
	 * 
	 * @param domain
	 *            Domain of the input text.
	 * @param reqAnnotator
	 *            Annotator that should be applied to the input text.
	 * @return Properties for the NLP pipeline.
	 */
	private Properties loadNlpProperties(String domain, String reqAnnotator) throws IOException {
		Properties prop = new Properties();
		String propFileName = domain + "_" + reqAnnotator + ".properties";

		try (InputStream is = getClass().getClassLoader().getResourceAsStream("corenlp-config/" + propFileName)) {
			prop.load(is);
		} catch (IOException e) {
			throw new IOException("Could not load '" + propFileName + "'.");
		}

		return prop;
	}
}
