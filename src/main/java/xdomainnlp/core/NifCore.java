package xdomainnlp.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import xdomainnlp.api.NifContext;
import xdomainnlp.api.NifEntity;
import xdomainnlp.api.NoveltyParameter;
import xdomainnlp.api.Post;
import xdomainnlp.api.NifSentenceImpl;
import xdomainnlp.api.NifTokenImpl;
import xdomainnlp.db.TriplestoreManagerImpl;
import xdomainnlp.entitylinking.AgdistisCore;
import xdomainnlp.interfaces.NifSentence;
import xdomainnlp.interfaces.NifToken;
import xdomainnlp.nullobjects.NullSentence;
import xdomainnlp.nullobjects.NullToken;

/**
 * NIF representation. Based on jplu's stanfordNLPRESTAPI.
 */
public class NifCore {

	private Post inputDoc;
	private Annotation coreNLPDoc;
	private NoveltyParameter noveltyParam;
	private AgdistisCore agdistis;
	private String requestedAnnotator;
	private String serviceBaseUri;
	private TriplestoreManagerImpl tripleStoreManager;

	public NifCore(Post inputDoc, Annotation coreNLPDoc, NoveltyParameter noveltyParam, AgdistisCore agdistis, String requestedAnnotator,
			String serviceBaseUri, TriplestoreManagerImpl tripleStoreManager) {
		this.inputDoc = inputDoc;
		this.coreNLPDoc = coreNLPDoc;
		this.noveltyParam = noveltyParam;
		this.agdistis = agdistis;
		this.requestedAnnotator = requestedAnnotator;
		this.serviceBaseUri = serviceBaseUri;
		this.tripleStoreManager = tripleStoreManager;
	}

	public NifContext buildContext() throws InterruptedException, IOException {
		final NifContext nifContext = new NifContext(inputDoc, noveltyParam, requestedAnnotator, serviceBaseUri, tripleStoreManager);

		this.buildSentencesFromContext(coreNLPDoc.get(CoreAnnotations.SentencesAnnotation.class), nifContext);

		return nifContext;
	}

	private void buildSentencesFromContext(final List<CoreMap> coreNlpSentences, final NifContext nifContext) throws InterruptedException, IOException {
		NifSentence sentence = new NifSentenceImpl(coreNlpSentences.get(0).get(CoreAnnotations.TextAnnotation.class), nifContext,
				coreNlpSentences.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
				coreNlpSentences.get(0).get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
				coreNlpSentences.get(0).get(CoreAnnotations.SentenceIndexAnnotation.class), NullSentence.getInstance());

		nifContext.addSentence(sentence);

		final List<NifSentence> sentencesList = new ArrayList<>();

		sentencesList.add(sentence);

		this.buildTokensFromSentence(coreNlpSentences.get(0), nifContext, sentence);
		this.buildEntitiesFromSentence(coreNlpSentences.get(0), nifContext, sentence);

		for (int i = 1; i < coreNlpSentences.size(); i++) {
			sentence = new NifSentenceImpl(coreNlpSentences.get(i).get(CoreAnnotations.TextAnnotation.class), nifContext,
					coreNlpSentences.get(i).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
					coreNlpSentences.get(i).get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
					coreNlpSentences.get(i).get(CoreAnnotations.SentenceIndexAnnotation.class), sentencesList.get(i - 1));

			nifContext.addSentence(sentence);
			sentencesList.add(sentence);
			this.buildTokensFromSentence(coreNlpSentences.get(i), nifContext, sentence);
			this.buildEntitiesFromSentence(coreNlpSentences.get(i), nifContext, sentence);
		}

		for (int i = 0; i < (sentencesList.size() - 1); i++) {
			sentencesList.get(i).setNextSentence(sentencesList.get(i + 1));
		}
	}

	private void buildTokensFromSentence(final CoreMap coreNlpSentence, final NifContext nifContext, final NifSentence sentence) {
		final CoreLabel firstLabel = coreNlpSentence.get(CoreAnnotations.TokensAnnotation.class).get(0);
		NifToken token = new NifTokenImpl(firstLabel.get(CoreAnnotations.TextAnnotation.class),
				firstLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class), firstLabel.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
				firstLabel.get(CoreAnnotations.CharacterOffsetEndAnnotation.class), firstLabel.get(CoreAnnotations.LemmaAnnotation.class),
				NullToken.getInstance(), nifContext, sentence, firstLabel.get(CoreAnnotations.IndexAnnotation.class));

		sentence.addToken(token);

		final List<NifToken> tokens = new ArrayList<>();

		tokens.add(token);

		for (int i = 1; i < coreNlpSentence.get(CoreAnnotations.TokensAnnotation.class).size(); i++) {
			final CoreLabel currentLabel = coreNlpSentence.get(CoreAnnotations.TokensAnnotation.class).get(i);
			token = new NifTokenImpl(currentLabel.get(CoreAnnotations.TextAnnotation.class),
					currentLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class),
					currentLabel.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
					currentLabel.get(CoreAnnotations.CharacterOffsetEndAnnotation.class), currentLabel.get(CoreAnnotations.LemmaAnnotation.class),
					tokens.get(i - 1), nifContext, sentence, currentLabel.get(CoreAnnotations.IndexAnnotation.class));

			sentence.addToken(token);
			tokens.add(token);
		}

		for (int i = 0; i < (tokens.size() - 1); i++) {
			tokens.get(i).setNextToken(tokens.get(i + 1));
		}
	}

	private void buildEntitiesFromSentence(final CoreMap coreNlpSentence, final NifContext nifContext, final NifSentence sentence)
			throws InterruptedException, IOException {
		for (final CoreMap entityMention : coreNlpSentence.get(CoreAnnotations.MentionsAnnotation.class)) {
			sentence.addEntity(new NifEntity(entityMention.get(CoreAnnotations.TextAnnotation.class),
					entityMention.get(CoreAnnotations.NamedEntityTagAnnotation.class), sentence, nifContext,
					entityMention.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
					entityMention.get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
					agdistis.getDisambiguatedUri(entityMention.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class))));
		}
	}
}
