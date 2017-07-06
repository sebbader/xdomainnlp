package xdomainnlp.interfaces;

import org.eclipse.rdf4j.model.Model;

public interface TriplestoreManager {

	void createRdf(String baseUri, String domain, String requestedAnnotator, String mongoDocId, Model rdfModel);

	Model readRdfDoc(String baseUri, String domain, String requestedAnnotator, String mongoDocId);

	Model readRdfSentences(String baseUri, String domain, String requestedAnnotator, String mongoDocId);

	Model readRdfSentence(String baseUri, String domain, String requestedAnnotator, String mongoDocId, int sentenceIndex);

	Model readRdfPosTags(String baseUri, String domain, String requestedAnnotator, String mongoDocId);

	Model readRdfEntities(String baseUri, String domain, String requestedAnnotator, String mongoDocId);

	String serializeRdfModel(Model rdfModel, String format);
}
