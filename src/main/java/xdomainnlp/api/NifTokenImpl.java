package xdomainnlp.api;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import xdomainnlp.interfaces.NifSentence;
import xdomainnlp.interfaces.NifToken;
import xdomainnlp.nullobjects.NullToken;

/**
 * NIF token corresponding to the CoreNLP annotations. Based on jplu's stanfordNLPRESTAPI.
 */
public class NifTokenImpl implements NifToken {

	private String tokenText;
	private String tokenTag;
	private int tokenBeginOffset;
	private int tokenEndOffset;
	private String lemma;
	private NifToken prevToken;
	private NifToken nextToken;
	private NifContext nifContext;
	private NifSentence sentence;
	private int tokenIndex;
	private ValueFactory vf;

	public NifTokenImpl(String tokenText, String tokenTag, int tokenBeginOffset, int tokenEndOffset, String lemma, NifToken prevToken, NifContext nifContext,
			NifSentence sentence, int tokenIndex) {
		this.tokenText = tokenText;
		this.tokenTag = tokenTag;
		this.tokenBeginOffset = tokenBeginOffset;
		this.tokenEndOffset = tokenEndOffset;
		this.lemma = lemma;
		this.prevToken = prevToken;
		this.nextToken = NullToken.getInstance();
		this.nifContext = nifContext;
		this.sentence = sentence;
		this.tokenIndex = tokenIndex;
		this.vf = SimpleValueFactory.getInstance();
	}

	/**
	 * Transform token into RDF model.
	 *
	 * @param serviceBaseUri
	 *            Host URI.
	 * @param domain
	 *            Current domain.
	 * @param mongoDocId
	 *            Assigned document ID.
	 *
	 * @return Entity RDF model.
	 */
	@Override
	public final Model createRDFModel(String serviceBaseUri, String domain, String mongoDocId) {
		Model rdfModel = new TreeModel();

		String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
		String base = serviceBaseUri + "nlp/" + domain + "/" + mongoDocId + "/";

		IRI tokenSubj = vf.createIRI(base, "token#char=" + tokenBeginOffset + "," + tokenEndOffset);

		rdfModel.add(tokenSubj, RDF.TYPE, vf.createIRI(nif, "String"));
		rdfModel.add(tokenSubj, RDF.TYPE, vf.createIRI(nif, "RFC5147String"));
		rdfModel.add(tokenSubj, RDF.TYPE, vf.createIRI(nif, "Word"));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "beginIndex"), vf.createLiteral(tokenBeginOffset));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "endIndex"), vf.createLiteral(tokenEndOffset));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "anchorOf"), vf.createLiteral(tokenText));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "sentence"),
				vf.createIRI(base, "sentence#char=" + sentence.getSentenceBeginOffset() + "," + sentence.getSentenceEndOffset()));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "referenceContext"),
				vf.createIRI(base, "context#char=" + nifContext.getInputDocBeginOffset() + "," + nifContext.getInputDocEndOffset()));
		rdfModel.add(tokenSubj, vf.createIRI(nif, "posTag"), vf.createLiteral(tokenTag));

		if (lemma != null) {
			rdfModel.add(tokenSubj, vf.createIRI(nif, "lemma"), vf.createLiteral(lemma));
		}

		if (this.nextToken.getTokenIndex() != -1) {
			rdfModel.add(tokenSubj, vf.createIRI(nif, "nextWord"),
					vf.createIRI(base, "token#char=" + nextToken.getTokenBeginOffset() + "," + nextToken.getTokenEndOffset()));
		}

		if (this.prevToken.getTokenIndex() != -1) {
			rdfModel.add(tokenSubj, vf.createIRI(nif, "previousWord"),
					vf.createIRI(base, "token#char=" + prevToken.getTokenBeginOffset() + "," + prevToken.getTokenEndOffset()));
		}

		return rdfModel;
	}

	@Override
	public final void setNextToken(final NifToken nextToken) {
		if (this.nextToken.getTokenIndex() == -1) {
			this.nextToken = nextToken;
		}
	}

	@Override
	public final int getTokenIndex() {
		return this.tokenIndex;
	}

	@Override
	public final String getTokenText() {
		return this.tokenText;
	}

	@Override
	public final int getTokenBeginOffset() {
		return this.tokenBeginOffset;
	}

	@Override
	public final int getTokenEndOffset() {
		return this.tokenEndOffset;
	}

}
