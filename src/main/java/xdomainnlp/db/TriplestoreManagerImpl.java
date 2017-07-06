package xdomainnlp.db;

import java.io.File;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import xdomainnlp.interfaces.TriplestoreManager;

public class TriplestoreManagerImpl implements TriplestoreManager {
	private Repository repo;
	private ValueFactory vf;
	private String nif;
	private String xsd;
	private String itsrdf;

	public TriplestoreManagerImpl(String dataDirPath) {
		this.nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
		this.xsd = XMLSchema.NAMESPACE;
		this.itsrdf = "http://www.w3.org/2005/11/its/rdf#";

		File dataDir = new File(dataDirPath);
		String indexes = "spoc,posc,cosp";

		if (!dataDir.isDirectory()) {
			dataDir.mkdirs();
		}

		this.vf = SimpleValueFactory.getInstance();
		this.repo = new SailRepository(new NativeStore(dataDir, indexes));
		repo.initialize();
	}

	@Override
	public void createRdf(String baseUri, String domain, String requestedAnnotator, String mongoDocId, Model rdfModel) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		IRI context = vf.createIRI(location);

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				repoCon.add(rdfModel, context);
				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}
	}

	@Override
	public Model readRdfDoc(String baseUri, String domain, String requestedAnnotator, String mongoDocId) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		String base = baseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = baseUri + "nlp/" + domain + "/" + "ontology/";
		Model modelResult = null;

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				String sparqlQuery = "CONSTRUCT { \n";
				sparqlQuery += "    ?s ?p ?o . \n";
				sparqlQuery += "} \n";
				sparqlQuery += "WHERE { \n";
				sparqlQuery += "    GRAPH <" + location + "> { \n";
				sparqlQuery += "        ?s ?p ?o . \n";
				sparqlQuery += "    } \n";
				sparqlQuery += "}";

				GraphQuery query = repoCon.prepareGraphQuery(sparqlQuery);
				modelResult = QueryResults.asModel(query.evaluate());

				if (!modelResult.isEmpty()) {
					modelResult.setNamespace("nif", nif);
					modelResult.setNamespace("xsd", xsd);
					modelResult.setNamespace("base", base);
					modelResult.setNamespace("local", local);

					if (modelResult.contains(null, vf.createIRI(itsrdf + "taIdentRef"), null)) {
						modelResult.setNamespace("itsrdf", itsrdf);
					}
				}

				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}

		return modelResult;
	}

	@Override
	public Model readRdfSentences(String baseUri, String domain, String requestedAnnotator, String mongoDocId) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		String base = baseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = baseUri + "nlp/" + domain + "/" + "ontology/";
		Model modelResult = null;

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				String sparqlQuery = "PREFIX nif: <" + nif + "> \n";
				sparqlQuery += "PREFIX local: <" + local + "> \n";
				sparqlQuery += "CONSTRUCT { \n";
				sparqlQuery += "    ?s nif:anchorOf ?o ; \n";
				sparqlQuery += "        local:index ?index . \n";
				sparqlQuery += "} \n";
				sparqlQuery += "WHERE { \n";
				sparqlQuery += "    GRAPH <" + location + "> { \n";
				sparqlQuery += "        ?s a nif:Sentence ; \n";
				sparqlQuery += "            nif:anchorOf ?o ; \n";
				sparqlQuery += "            local:index ?index . \n";
				sparqlQuery += "    } \n";
				sparqlQuery += "}";

				GraphQuery query = repoCon.prepareGraphQuery(sparqlQuery);
				modelResult = QueryResults.asModel(query.evaluate());

				if (!modelResult.isEmpty()) {
					modelResult.setNamespace("nif", nif);
					modelResult.setNamespace("xsd", xsd);
					modelResult.setNamespace("base", base);
					modelResult.setNamespace("local", local);
				}

				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}

		return modelResult;
	}

	@Override
	public Model readRdfSentence(String baseUri, String domain, String requestedAnnotator, String mongoDocId, int sentenceIndex) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		String local = baseUri + "nlp/" + domain + "/" + "ontology/";
		Model modelResult = null;

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				String sparqlQuery = "PREFIX nif: <" + nif + "> \n";
				sparqlQuery += "PREFIX xsd: <" + xsd + "> \n";
				sparqlQuery += "PREFIX local: <" + local + "> \n";
				sparqlQuery += "CONSTRUCT { \n";
				sparqlQuery += "    ?s local:index ?o . \n";
				sparqlQuery += "} \n";
				sparqlQuery += "WHERE { \n";
				sparqlQuery += "    GRAPH <" + location + "> { \n";
				sparqlQuery += "        ?s local:index " + sentenceIndex + " . \n";
				sparqlQuery += "        ?o a xsd:int . \n";
				sparqlQuery += "    } \n";
				sparqlQuery += "}";

				GraphQuery query = repoCon.prepareGraphQuery(sparqlQuery);
				modelResult = QueryResults.asModel(query.evaluate());
				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}

		return modelResult;
	}

	@Override
	public Model readRdfPosTags(String baseUri, String domain, String requestedAnnotator, String mongoDocId) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		String base = baseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		Model modelResult = null;

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				String sparqlQuery = "PREFIX nif: <" + nif + "> \n";
				sparqlQuery += "CONSTRUCT { \n";
				sparqlQuery += "    ?s nif:posTag ?o . \n";
				sparqlQuery += "} \n";
				sparqlQuery += "WHERE { \n";
				sparqlQuery += "    GRAPH <" + location + "> { \n";
				sparqlQuery += "        ?s a nif:Word . \n";
				sparqlQuery += "        ?s nif:posTag ?o . \n";
				sparqlQuery += "    } \n";
				sparqlQuery += "}";

				GraphQuery query = repoCon.prepareGraphQuery(sparqlQuery);
				modelResult = QueryResults.asModel(query.evaluate());

				if (!modelResult.isEmpty()) {
					modelResult.setNamespace("nif", nif);
					modelResult.setNamespace("base", base);
				}

				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}

		return modelResult;
	}

	@Override
	public Model readRdfEntities(String baseUri, String domain, String requestedAnnotator, String mongoDocId) {
		String location = baseUri + "nlp/" + domain + "/" + requestedAnnotator + "/" + mongoDocId;
		String base = baseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = baseUri + "nlp/" + domain + "/" + "ontology/";
		Model modelResult = null;

		try (RepositoryConnection repoCon = repo.getConnection()) {
			repoCon.begin();
			try {
				String sparqlQuery = "PREFIX nif: <" + nif + "> \n";
				sparqlQuery += "PREFIX local: <" + local + "> \n";
				sparqlQuery += "CONSTRUCT { \n";
				sparqlQuery += "    ?s nif:anchorOf ?o ; \n";
				sparqlQuery += "        local:type ?type . \n";
				sparqlQuery += "} \n";
				sparqlQuery += "WHERE { \n";
				sparqlQuery += "    GRAPH <" + location + "> { \n";
				sparqlQuery += "        ?s a nif:Phrase ; \n";
				sparqlQuery += "            nif:anchorOf ?o ; \n";
				sparqlQuery += "            local:type ?type . \n";
				sparqlQuery += "    } \n";
				sparqlQuery += "}";

				GraphQuery query = repoCon.prepareGraphQuery(sparqlQuery);
				modelResult = QueryResults.asModel(query.evaluate());

				if (!modelResult.isEmpty()) {
					modelResult.setNamespace("nif", nif);
					modelResult.setNamespace("base", base);
					modelResult.setNamespace("local", local);
				}

				repoCon.commit();
			} catch (RepositoryException e) {
				repoCon.rollback();
			}
		}

		return modelResult;
	}

	@Override
	public final String serializeRdfModel(Model rdfModel, String format) {
		StringWriter stringWriter = new StringWriter();
		RDFWriter rdfWriter = null;

		if (format.equals("json-ld")) {
			rdfWriter = Rio.createWriter(RDFFormat.JSONLD, stringWriter);
			rdfWriter.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
		} else if (format.equals("turtle")) {
			rdfWriter = Rio.createWriter(RDFFormat.TURTLE, stringWriter);
		}

		rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true);

		Rio.write(rdfModel, rdfWriter);

		if (!rdfModel.isEmpty()) {
			return stringWriter.toString();
		} else {
			return null;
		}
	}
}
