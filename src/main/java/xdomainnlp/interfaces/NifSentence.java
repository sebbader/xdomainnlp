package xdomainnlp.interfaces;

import java.util.List;

import org.eclipse.rdf4j.model.Model;

import xdomainnlp.api.NifEntity;

public interface NifSentence {
	void addToken(NifToken token);

	void addEntity(NifEntity entity);

	void setNextSentence(NifSentence nextSentence);

	List<NifEntity> getEntities();

	int getSentenceIndex();

	int getSentenceBeginOffset();

	int getSentenceEndOffset();

	Model createRdfModel(String serviceBaseUri, String domain, String requestedAnnotator, String mongoDocId);
}
