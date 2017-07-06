package xdomainnlp.nullobjects;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdomainnlp.interfaces.NifToken;

/**
 * Null object representing a null token. Based on jplu's stanfordNLPRESTAPI.
 */
public final class NullToken implements NifToken {
	static final Logger LOGGER = LoggerFactory.getLogger(NullToken.class);
	private static final NullToken INSTANCE = new NullToken();

	public static NullToken getInstance() {
		return NullToken.INSTANCE;
	}

	private NullToken() {

	}

	@Override
	public void setNextToken(final NifToken newNextToken) {
		throw new UnsupportedOperationException("Not imlpemented");
	}

	@Override
	public int getTokenIndex() {
		return -1;
	}

	@Override
	public String getTokenText() {
		return null;
	}

	@Override
	public int getTokenBeginOffset() {
		return 0;
	}

	@Override
	public int getTokenEndOffset() {
		return 0;
	}

	@Override
	public Model createRDFModel(final String host, final String domain, final String mongoDocId) {
		TreeModel rdfModel = new TreeModel();

		return rdfModel;
	}
}
