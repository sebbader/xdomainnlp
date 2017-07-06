package xdomainnlp.namedentityrecognition;

import java.util.ArrayList;
import rbbnpe.BaseNounPhrase;
import rbbnpe.POSBasedBaseNounPhraseExtractor;

public class NounPhraseExtractor {

	public ArrayList<BaseNounPhrase> extractNP(String text) {
		POSBasedBaseNounPhraseExtractor baseNounPhraseExtractor = new POSBasedBaseNounPhraseExtractor(
				"edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
		baseNounPhraseExtractor.extractBaseNounPhrasesFromText(text);

		return baseNounPhraseExtractor.getBaseNounPhrases();
	}
}
