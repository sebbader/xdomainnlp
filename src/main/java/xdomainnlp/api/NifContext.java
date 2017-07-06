package xdomainnlp.api;

import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;

import xdomainnlp.db.TriplestoreManagerImpl;
import xdomainnlp.interfaces.NifSentence;

/**
 * NIF context corresponding to the CoreNLP annotations. Based on jplu's stanfordNLPRESTAPI.
 */
public class NifContext {
	private Post inputDoc;
	private NoveltyParameter noveltyParam;
	private String reqAnnotator;
	private String serviceBaseUri;
	private String domain;
	private String mongoDocId;
	private List<NifSentence> sentences;
	private int docBeginOffset;
	private int docEndOffset;
	private TriplestoreManagerImpl tripleStoreManager;
	private ValueFactory valueFactory;

	public NifContext(Post inputDoc, NoveltyParameter noveltyParam, String reqAnnotator, String serviceBaseUri,
			TriplestoreManagerImpl tripleStoreManager) {
		this.inputDoc = inputDoc;
		this.noveltyParam = noveltyParam;
		this.reqAnnotator = reqAnnotator;
		this.serviceBaseUri = serviceBaseUri;
		this.domain = inputDoc.getDomain();
		this.mongoDocId = inputDoc.getMongoDocId();
		this.sentences = new ArrayList<>();
		this.docBeginOffset = 0;
		this.docEndOffset = inputDoc.getBody().length();
		this.tripleStoreManager = tripleStoreManager;
		this.valueFactory = SimpleValueFactory.getInstance();
	}

	/**
	 * Transform NIF context into RDF model.
	 *
	 * @return Context RDF model.
	 */
	public Model createRDFModel() {
		Model rdfModel = new TreeModel();

		String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
		String xsd = "http://www.w3.org/2001/XMLSchema#";
		String base = serviceBaseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = serviceBaseUri + "nlp/" + domain + "/" + "ontology/";

		rdfModel.setNamespace("nif", nif);
		rdfModel.setNamespace("xsd", xsd);
		rdfModel.setNamespace("base", base);

		IRI contextSubj = valueFactory.createIRI(base, "context#char=" + docBeginOffset + "," + docEndOffset);

		rdfModel.add(contextSubj, RDF.TYPE, valueFactory.createIRI(nif, "String"));
		rdfModel.add(contextSubj, RDF.TYPE, valueFactory.createIRI(nif, "RFC5147String"));
		rdfModel.add(contextSubj, RDF.TYPE, valueFactory.createIRI(nif, "Context"));
		rdfModel.add(contextSubj, valueFactory.createIRI(nif, "beginIndex"), valueFactory.createLiteral(docBeginOffset));
		rdfModel.add(contextSubj, valueFactory.createIRI(nif, "endIndex"), valueFactory.createLiteral(docEndOffset));
		rdfModel.add(contextSubj, valueFactory.createIRI(nif, "isString"), valueFactory.createLiteral(inputDoc.getBody()));

		for (NifSentence sentence : sentences) {
			Model tmpModel = sentence.createRdfModel(serviceBaseUri, domain, reqAnnotator, mongoDocId);
			rdfModel.addAll(tmpModel);
			tmpModel.getNamespaces().stream().filter(ns -> !rdfModel.getNamespace(ns.getPrefix()).isPresent()).forEach(rdfModel::setNamespace);
		}

		if (noveltyParam.isNoveltyComputed()) {
			rdfModel.setNamespace("local", local);
			IRI noveltySubj = valueFactory.createIRI(base, "novelty#char=" + docBeginOffset + "," + docEndOffset);

			rdfModel.add(noveltySubj, valueFactory.createIRI(local, "isNovel"), valueFactory.createLiteral(inputDoc.isNovel()));
			rdfModel.add(noveltySubj, valueFactory.createIRI(local, "noveltyScore"), valueFactory.createLiteral(inputDoc.getNoveltyScore()));
			rdfModel.add(noveltySubj, valueFactory.createIRI(local, "weight"), valueFactory.createLiteral(noveltyParam.getWeight()));
			rdfModel.add(noveltySubj, valueFactory.createIRI(local, "threshold"), valueFactory.createLiteral(noveltyParam.getThreshold()));
		}

		tripleStoreManager.createRdf(serviceBaseUri, domain, reqAnnotator, mongoDocId, rdfModel);

		return rdfModel;
	}

	public void saveRdfModelInTripleStore(Model rdfModel) {
		tripleStoreManager.createRdf(serviceBaseUri, domain, reqAnnotator, mongoDocId, rdfModel);
	}

	/**
	 * Serialize RDF model.
	 *
	 * @param rdfModel
	 *            Model that should be serialized.
	 * @param rdfFormat
	 *            Required RDF format.
	 * 
	 * @return RDF string in the given format.
	 */
	public String writeRdfModel(Model rdfModel, RDFFormat rdfFormat) {
		StringWriter stringWriter = new StringWriter();
		RDFWriter rdfWriter = Rio.createWriter(rdfFormat, stringWriter);

		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);

		if (rdfFormat.equals(RDFFormat.JSONLD)) {
			rdfWriter.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
		}

		Rio.write(rdfModel, rdfWriter);

		return stringWriter.toString();
	}

	public String getInputDocText() {
		return this.inputDoc.getBody();
	}

	public void addSentence(NifSentence sentence) {
		this.sentences.add(sentence);
	}

	public List<NifSentence> getSentences() {
		return Collections.unmodifiableList(this.sentences);
	}

	public int getInputDocBeginOffset() {
		return this.docBeginOffset;
	}

	public int getInputDocEndOffset() {
		return this.docEndOffset;
	}
}
