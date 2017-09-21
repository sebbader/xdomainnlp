package xdomainnlp.entitylinking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.DocumentText;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class AgdistisCore {
	private final Annotation coreNlpDoc;
	private final String inputDocText;
	public HashMap<Integer, String> disambiguatedUris;
	

	public AgdistisCore(Annotation coreNlpDoc, String inputDocText) {
		this.coreNlpDoc = coreNlpDoc;
		this.inputDocText = inputDocText;
		this.disambiguatedUris = new HashMap<Integer, String>();
	}

	public void disambiguateEntities() throws InterruptedException, IOException {
		NEDAlgo_HITS hits = new NEDAlgo_HITS();
		Document agdistisDoc = new Document();
		ArrayList<NamedEntityInText> namedEntityInTextList = new ArrayList<NamedEntityInText>();

		for (CoreMap sentence : coreNlpDoc.get(SentencesAnnotation.class)) {
			for (CoreMap entityMention : sentence.get(MentionsAnnotation.class)) {
				namedEntityInTextList.add(new NamedEntityInText(entityMention.get(CharacterOffsetBeginAnnotation.class),
						entityMention.get(TextAnnotation.class).length(), entityMention.get(TextAnnotation.class)));
			}
		}
		for (NamedEntityInText entity : namedEntityInTextList) {
		}

		NamedEntitiesInText namedEntitiesInText = new NamedEntitiesInText(namedEntityInTextList);
		DocumentText documentText = new DocumentText(inputDocText);

		agdistisDoc.addText(documentText);
		agdistisDoc.addNamedEntitiesInText(namedEntitiesInText);

		hits.run(agdistisDoc, null);

		NamedEntitiesInText namedEntitiesInTextResult = agdistisDoc.getNamedEntitiesInText();

		for (NamedEntityInText namedEntityInText : namedEntitiesInTextResult) {
			String disambiguatedUri = namedEntityInText.getNamedEntityUri();
			this.disambiguatedUris.put(namedEntityInText.getStartPos(), disambiguatedUri);
		}
	}

	public String getDisambiguatedUri(int characterOffsetBeginIndex) {
		return this.disambiguatedUris.get(characterOffsetBeginIndex);
	}
}
