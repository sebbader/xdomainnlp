package xdomainnlp.nullobjects;

import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdomainnlp.api.NifEntity;
import xdomainnlp.interfaces.NifSentence;
import xdomainnlp.interfaces.NifToken;

/**
 * Null object representing a null sentence. Based on jplu's stanfordNLPRESTAPI.
 */
public final class NullSentence implements NifSentence {
	static final Logger LOGGER = LoggerFactory.getLogger(NullSentence.class);
	private static final NullSentence INSTANCE = new NullSentence();

	public static NullSentence getInstance() {
		return NullSentence.INSTANCE;
	}

	private NullSentence() {

	}

	@Override
	public void addToken(final NifToken newToken) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void addEntity(final NifEntity newEntity) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public void setNextSentence(final NifSentence newNextSentence) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public List<NifEntity> getEntities() {
		return null;
	}

	@Override
	public int getSentenceIndex() {
		return -1;
	}

	@Override
	public int getSentenceBeginOffset() {
		return 0;
	}

	@Override
	public int getSentenceEndOffset() {
		return 0;
	}

	@Override
	public Model createRdfModel(final String host, final String domain, final String requestedAnnotator, final String mongoDocId) {
		TreeModel rdfModel = new TreeModel();

		return rdfModel;
	}
}
