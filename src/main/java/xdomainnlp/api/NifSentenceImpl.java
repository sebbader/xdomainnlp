package xdomainnlp.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import xdomainnlp.exceptions.InexistentNlpProcessException;
import xdomainnlp.interfaces.NifSentence;
import xdomainnlp.interfaces.NifToken;
import xdomainnlp.nullobjects.NullSentence;
import xdomainnlp.nullobjects.NullToken;

/**
 * NIF sentence corresponding to the CoreNLP annotations. Based on jplu's stanfordNLPRESTAPI.
 */
public class NifSentenceImpl implements NifSentence {
	private String sentenceText;
	private NifContext nifContext;
	private List<NifToken> tokens;
	private List<NifEntity> entities;
	private NifSentence nextSentence;
	private NifSentence prevSentence;
	private NifToken firstToken;
	private NifToken lastToken;
	private int sentenceBeginOffset;
	private int sentenceEndOffset;
	private int sentenceIndex;
	private ValueFactory vf;

	public NifSentenceImpl(String sentenceText, NifContext nifContext, int sentenceBeginOffset, int sentenceEndOffset, int sentenceIndex,
			NifSentence prevSentence) {
		this.tokens = new ArrayList<>();
		this.entities = new ArrayList<>();
		this.nextSentence = NullSentence.getInstance();
		this.prevSentence = prevSentence;
		this.sentenceText = sentenceText;
		this.nifContext = nifContext;
		this.sentenceBeginOffset = sentenceBeginOffset;
		this.sentenceEndOffset = sentenceEndOffset;
		this.sentenceIndex = sentenceIndex;
		this.firstToken = NullToken.getInstance();
		this.lastToken = NullToken.getInstance();
		this.vf = SimpleValueFactory.getInstance();
	}

	/**
	 * Transform sentence into RDF model.
	 *
	 * @param serviceBaseUri
	 *            Host URI.
	 * @param domain
	 *            Current domain.
	 * @param requestedAnnotator
	 *            Annotator that has been requested.
	 * @param mongoDocId
	 *            Assigned document ID.
	 *
	 * @return Sentence RDF model.
	 */
	@Override
	public final Model createRdfModel(String serviceBaseUri, String domain, String requestedAnnotator, String mongoDocId) {
		Model rdfModel = new TreeModel();

		String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
		String base = serviceBaseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = serviceBaseUri + "nlp/" + domain + "/" + "ontology/";

		rdfModel.setNamespace("local", local);

		IRI sentenceSubj = vf.createIRI(base, "sentence#char=" + sentenceBeginOffset + "," + sentenceEndOffset);

		rdfModel.add(sentenceSubj, RDF.TYPE, vf.createIRI(nif, "String"));
		rdfModel.add(sentenceSubj, RDF.TYPE, vf.createIRI(nif, "RFC5147String"));
		rdfModel.add(sentenceSubj, RDF.TYPE, vf.createIRI(nif, "Sentence"));
		rdfModel.add(sentenceSubj, vf.createIRI(nif, "beginIndex"), vf.createLiteral(sentenceBeginOffset));
		rdfModel.add(sentenceSubj, vf.createIRI(nif, "endIndex"), vf.createLiteral(sentenceEndOffset));
		rdfModel.add(sentenceSubj, vf.createIRI(nif, "referenceContext"),
				vf.createIRI(base, "context#char=" + nifContext.getInputDocBeginOffset() + "," + nifContext.getInputDocEndOffset()));
		rdfModel.add(sentenceSubj, vf.createIRI(nif, "anchorOf"), vf.createLiteral(sentenceText));

		if (requestedAnnotator.equals("pos")) {
			for (final NifToken token : this.tokens) {
				rdfModel.add(sentenceSubj, vf.createIRI(nif, "word"), vf.createIRI(base, "token#char=" + token.getTokenBeginOffset() + "," + token.getTokenEndOffset()));

				Model tmpModel = token.createRDFModel(serviceBaseUri, domain, mongoDocId);
				rdfModel.addAll(tmpModel);
				tmpModel.getNamespaces().stream().filter(ns -> !rdfModel.getNamespace(ns.getPrefix()).isPresent()).forEach(rdfModel::setNamespace);
			}

			rdfModel.add(sentenceSubj, vf.createIRI(nif, "firstToken"),
					vf.createIRI(base, "token#char=" + firstToken.getTokenBeginOffset() + "," + firstToken.getTokenEndOffset()));
			rdfModel.add(sentenceSubj, vf.createIRI(nif, "lastToken"), vf.createIRI(base, "token#char=" + lastToken.getTokenBeginOffset() + "," + lastToken.getTokenEndOffset()));
		} else if (requestedAnnotator.equals("ner")) {
			for (NifEntity entity : entities) {
				rdfModel.add(sentenceSubj, vf.createIRI(local, "entity"),
						vf.createIRI(base, "entity#char=" + entity.getEntityBeginOffset() + ',' + entity.getEntityEndOffset()));

				Model tmpModel = entity.createRDFModel(serviceBaseUri, domain, mongoDocId);
				rdfModel.addAll(tmpModel);
				tmpModel.getNamespaces().stream().filter(ns -> !rdfModel.getNamespace(ns.getPrefix()).isPresent()).forEach(rdfModel::setNamespace);
			}
		} else {
			throw new InexistentNlpProcessException(requestedAnnotator + " is not a valid NLP Process");
		}

		if (this.nextSentence.getSentenceIndex() != -1) {
			rdfModel.add(sentenceSubj, vf.createIRI(nif, "nextSentence"),
					vf.createIRI(base, "sentence#char=" + nextSentence.getSentenceBeginOffset() + "," + nextSentence.getSentenceEndOffset()));
		}

		if (this.prevSentence.getSentenceIndex() != -1) {
			rdfModel.add(sentenceSubj, vf.createIRI(nif, "previousSentence"),
					vf.createIRI(base, "sentence#char=" + prevSentence.getSentenceBeginOffset() + "," + prevSentence.getSentenceEndOffset()));
		}

		rdfModel.add(sentenceSubj, vf.createIRI(local, "index"), vf.createLiteral(sentenceIndex));

		return rdfModel;
	}

	@Override
	public final void addToken(final NifToken token) {
		if (this.tokens.isEmpty()) {
			this.firstToken = token;
		}

		this.tokens.add(token);
		this.lastToken = token;
	}

	@Override
	public final void addEntity(final NifEntity entity) {
		this.entities.add(entity);
	}

	@Override
	public void setNextSentence(final NifSentence nextSentence) {
		if (this.nextSentence.getSentenceIndex() == -1) {
			this.nextSentence = nextSentence;
		}
	}

	@Override
	public List<NifEntity> getEntities() {
		return Collections.unmodifiableList(this.entities);
	}

	@Override
	public int getSentenceIndex() {
		return this.sentenceIndex;
	}

	@Override
	public int getSentenceBeginOffset() {
		return this.sentenceBeginOffset;
	}

	@Override
	public int getSentenceEndOffset() {
		return this.sentenceEndOffset;
	}

}
