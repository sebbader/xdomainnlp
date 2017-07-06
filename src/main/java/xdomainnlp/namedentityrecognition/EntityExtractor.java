package xdomainnlp.namedentityrecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import rbbnpe.BaseNounPhrase;
import xdomainnlp.api.Entity;

public class EntityExtractor {
	private NounPhraseExtractor npe;
	private Annotation coreNlpDoc;
	private ArrayList<Entity> entities;
	private ArrayList<BaseNounPhrase> nounPhrases;

	public EntityExtractor(String inputText, Annotation coreNlpDoc, ArrayList<Entity> entities) {
		this.npe = new NounPhraseExtractor();
		this.coreNlpDoc = coreNlpDoc;
		this.entities = entities;
		this.nounPhrases = npe.extractNP(inputText);
	}

	public void extractEntities() {
		for (BaseNounPhrase nounPhrase : nounPhrases) {
			Entity entity = new Entity(null, nounPhrase, isNamedEntity(nounPhrase) || isNamedEntityByCounting(nounPhrase));
			entities.add(entity);
			resolveSynonyms(entity);
		}
	}

	private boolean isNamedEntity(BaseNounPhrase nounPhrase) {
		boolean isNamedEntity = true;

		if (!nounPhrase.getPosTag().matches("NN|NNP")) {
			isNamedEntity = false;
		}

		String[] nounPhraseParts = nounPhrase.getPhraseString().split(" ");

		if (!nounPhraseParts[nounPhraseParts.length - 1].equals(nounPhrase.getHead())) {
			isNamedEntity = false;
		}

		String[] nounPhrasePartsWithPosTag = nounPhrase.getPhraseStringWithPOSTags().split(" ");

		for (int i = 0; i < nounPhrasePartsWithPosTag.length; i++) {
			String nounPhrasePartPosTag = nounPhrasePartsWithPosTag[i].substring(nounPhrasePartsWithPosTag[i].lastIndexOf("/") + 1,
					nounPhrasePartsWithPosTag[i].length());
			char firstChar = nounPhraseParts[i].charAt(0);

			if (nounPhrasePartPosTag.matches("NN|NNS|NNP|NNPS")) {
				if (Character.isLowerCase(firstChar)) {
					isNamedEntity = false;
				}
			} else if (nounPhrasePartPosTag.matches("IN|DT|CC|WDT|RB")) {
				if (Character.isUpperCase(firstChar)) {
					isNamedEntity = false;
				}
			} else {
				isNamedEntity = false;
			}
		}

		return isNamedEntity;
	}

	private boolean isNamedEntityByCounting(BaseNounPhrase nounPhrase) {
		ArrayList<Integer> sentenceBeginOffsets = new ArrayList<>();
		ArrayList<BaseNounPhrase> identicalNounPhrases = new ArrayList<>();
		int nounPhraseCapitalizedOccurance = 0;

		for (CoreMap sentence : coreNlpDoc.get(SentencesAnnotation.class)) {
			sentenceBeginOffsets.add(sentence.get(CharacterOffsetBeginAnnotation.class));
		}

		for (BaseNounPhrase np : nounPhrases) {
			if (!sentenceBeginOffsets.contains(nounPhrase.getStartOffset())) {
				if (np.getPhraseString().equalsIgnoreCase(nounPhrase.getPhraseString())) {
					identicalNounPhrases.add(np);
				}
			} else {
				return isNamedEntity(nounPhrase);
			}
		}

		for (BaseNounPhrase np : identicalNounPhrases) {
			boolean isCapitalized = true;
			String[] nounPhrasePartsWithPosTag = np.getPhraseStringWithPOSTags().split(" ");

			for (int i = 0; i < nounPhrasePartsWithPosTag.length; i++) {
				String nounPhrasePartPosTag = nounPhrasePartsWithPosTag[i].substring(nounPhrasePartsWithPosTag[i].lastIndexOf("/") + 1,
						nounPhrasePartsWithPosTag[i].length());
				char firstChar = nounPhrasePartsWithPosTag[i].charAt(0);

				if (nounPhrasePartPosTag.matches("NN|NNS|NNP|NNPS")) {
					if (Character.isLowerCase(firstChar)) {
						isCapitalized = false;
					}
				}
			}

			if (isCapitalized) {
				nounPhraseCapitalizedOccurance += 1;
			}
		}

		if ((double) nounPhraseCapitalizedOccurance / identicalNounPhrases.size() > 0.7) {
			return true;
		} else {
			return false;
		}
	}

	private void resolveSynonyms(Entity entity) {
		Map<Integer, CorefChain> corefChains = coreNlpDoc.get(CorefChainAnnotation.class);

		corefChainLoop: for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
			HashMap<Integer, String> synonyms = new HashMap<>();

			CorefChain corefChain = entry.getValue();
			CorefMention representativeMention = corefChain.getRepresentativeMention();
			List<CoreLabel> tokens = coreNlpDoc.get(SentencesAnnotation.class).get(representativeMention.sentNum - 1).get(TokensAnnotation.class);

			if (corefChain.getMentionsInTextualOrder().size() <= 1) {
				continue corefChainLoop;
			}

			String representativeMentionString = "";

			for (int i = representativeMention.startIndex - 1; i < representativeMention.endIndex - 1; i++) {
				representativeMentionString += tokens.get(i).get(TextAnnotation.class) + " ";
			}

			representativeMentionString = representativeMentionString.trim();

			synonyms.put(tokens.get(representativeMention.startIndex - 1).get(CharacterOffsetBeginAnnotation.class), representativeMentionString);

			corefMentionLoop: for (CorefMention corefMention : corefChain.getMentionsInTextualOrder()) {
				String corefMentionString = "";
				tokens = coreNlpDoc.get(SentencesAnnotation.class).get(corefMention.sentNum - 1).get(TokensAnnotation.class);

				for (int i = corefMention.startIndex - 1; i < corefMention.endIndex - 1; i++) {
					if (tokens.get(i).get(PartOfSpeechAnnotation.class).matches("DT|WDT|PRP\\$")) {
						continue corefMentionLoop;
					} else if (!tokens.get(i).get(PartOfSpeechAnnotation.class).matches("POS")) {
						corefMentionString += tokens.get(i).get(TextAnnotation.class) + " ";
					}
				}

				corefMentionString = corefMentionString.trim();

				if (representativeMentionString.equals(corefMentionString)) {
					continue corefMentionLoop;
				}

				synonyms.put(tokens.get(corefMention.startIndex - 1).get(CharacterOffsetBeginAnnotation.class), corefMentionString);
			}

			HashMap<Integer, ArrayList<String>> transformedSynonyms = transformSynonyms(synonyms);

			if (transformedSynonyms.containsKey(entity.getNounPhrase().getStartOffset())) {
				entity.setCorefSynonyms(transformedSynonyms.get(entity.getNounPhrase().getStartOffset()));
			}
		}
	}

	private HashMap<Integer, ArrayList<String>> transformSynonyms(HashMap<Integer, String> synonymsToTransform) {
		HashMap<Integer, ArrayList<String>> result = new HashMap<>();

		for (Map.Entry<Integer, String> entry : synonymsToTransform.entrySet()) {
			ArrayList<String> mentions = new ArrayList<>();
			for (Map.Entry<Integer, String> tmp : synonymsToTransform.entrySet()) {
				if (tmp.getKey() == entry.getKey()) {
					continue;
				} else {
					mentions.add(tmp.getValue());
				}
			}
			result.put(entry.getKey(), mentions);
		}

		return result;
	}
}
