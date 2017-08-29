package xdomainnlp.namedentityrecognition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.model.vocabulary.OWL;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.LabeledChunkIdentifier;
import edu.stanford.nlp.util.CoreMap;
import xdomainnlp.api.Entity;

public class AnnotationMerger {
	public static final String PROPERTIES_FILE_ONTOLOGY = "ontology";
	public static final String PROPERTIES_FILE_INFERRED_ONTOLOGY = "inferred-ontology";
	public static final String PROPERTIES_FILE_NAMED_ENTITY_TYPES = "named-entity-types";

	private String domain;
	private String inputText;
	private Annotation coreNlpDoc;

	public AnnotationMerger(String domain, String inputText, Annotation coreNlpDoc) throws IOException {
		this.domain = domain;
		this.inputText = inputText;
		this.coreNlpDoc = coreNlpDoc;
	}

	public void mergeAnnotations() throws IOException {
		ArrayList<Entity> entities = new ArrayList<>();

		EntityExtractor ha = new EntityExtractor(inputText, coreNlpDoc, entities);
		ha.extractEntities();

		EntityClassificator ec = new EntityClassificator(domain, coreNlpDoc, entities);
		ec.classifyEntities("http://purl.obolibrary.org/obo/IAO_0000115");

		addCoreNlpTypes(entities);
	}

	public void addCoreNlpTypes(ArrayList<Entity> entities) {
		Properties namedEntityTypesProps = loadProperties(PROPERTIES_FILE_NAMED_ENTITY_TYPES);
		List<CoreMap> sentences = coreNlpDoc.get(SentencesAnnotation.class);

		for (int i = 0; i < sentences.size(); i++) {
			List<CoreMap> mentions = sentences.get(i).get(MentionsAnnotation.class);
			List<CoreLabel> tokens = sentences.get(i).get(TokensAnnotation.class);

			for (int j = 0; j < mentions.size(); j++) {
				for (Entity entity : entities) {
					int mentionBeginOffset = mentions.get(j).get(CharacterOffsetBeginAnnotation.class);
					int mentionEndOffset = mentions.get(j).get(CharacterOffsetEndAnnotation.class);
					int entityBeginOffset = entity.getNounPhrase().getStartOffset();
					int entityEndOffset = entityBeginOffset + entity.getNounPhrase().getPhraseString().length();
					
					
					// find entity type by relying on Stanford Core Annotations ONLY!!!
					// TODO enhancing for classes and superclasses in custom ontology
					String namedEntityType = mentions.get(j).get(NamedEntityTagAnnotation.class);

					if ((mentionBeginOffset < entityBeginOffset && mentionEndOffset > entityEndOffset) && !namedEntityType.equals("O")) {
						entity.setType(namedEntityTypesProps.getProperty(namedEntityType));
					}
				}
			}

			for (int k = 0; k < tokens.size(); k++) {
				int tokenBeginOffset = tokens.get(k).get(CharacterOffsetBeginAnnotation.class);
				int tokenEndOffset = tokens.get(k).get(CharacterOffsetEndAnnotation.class);
				String namedEntityType = tokens.get(k).get(NamedEntityTagAnnotation.class);

				if (!namedEntityType.equals("O")) {
					tokens.get(k).set(NamedEntityTagAnnotation.class, namedEntityTypesProps.getProperty(namedEntityType));
				}

				for (Entity entity : entities) {
					int entityBeginOffset = entity.getNounPhrase().getStartOffset();
					int entityEndOffset = entityBeginOffset + entity.getNounPhrase().getPhraseString().length();

					if ((tokenBeginOffset == entityBeginOffset) || (tokenBeginOffset > entityBeginOffset && tokenEndOffset <= entityEndOffset)) {
						if (entity.getType() != null) {
							tokens.get(k).set(NamedEntityTagAnnotation.class, entity.getType());
						} else if (entity.getType() == null && entity.isNamedEntity() && namedEntityType.equals("O")) {
							entity.setType(OWL.THING.stringValue());
							tokens.get(k).set(NamedEntityTagAnnotation.class, entity.getType());
						}
					}
				}
			}
		}

		LabeledChunkIdentifier chunkIdentifier = new LabeledChunkIdentifier();

		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			Integer tokenBegin = sentence.get(TokenBeginAnnotation.class);

			if (tokenBegin == null) {
				tokenBegin = 0;
			}

			List<CoreMap> chunks = chunkIdentifier.getAnnotatedChunks(tokens, tokenBegin, TextAnnotation.class, NamedEntityTagAnnotation.class);
			sentence.set(CoreAnnotations.MentionsAnnotation.class, chunks);
		}
	}

	public void setCoreNlpTypes() {
		Properties namedEntityTypesProps = loadProperties(AnnotationMerger.PROPERTIES_FILE_NAMED_ENTITY_TYPES);
		List<CoreMap> sentences = coreNlpDoc.get(SentencesAnnotation.class);

		for (int i = 0; i < sentences.size(); i++) {
			List<CoreLabel> tokens = sentences.get(i).get(TokensAnnotation.class);

			for (int j = 0; j < tokens.size(); j++) {
				String namedEntityType = tokens.get(j).get(NamedEntityTagAnnotation.class);

				if (!namedEntityType.equals("O")) {
					tokens.get(j).set(NamedEntityTagAnnotation.class, namedEntityTypesProps.getProperty(namedEntityType));
				}
			}
		}

		LabeledChunkIdentifier chunkIdentifier = new LabeledChunkIdentifier();

		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			Integer tokenBegin = sentence.get(TokenBeginAnnotation.class);

			if (tokenBegin == null) {
				tokenBegin = 0;
			}

			List<CoreMap> chunks = chunkIdentifier.getAnnotatedChunks(tokens, tokenBegin, TextAnnotation.class, NamedEntityTagAnnotation.class);
			sentence.set(CoreAnnotations.MentionsAnnotation.class, chunks);
		}
	}

	private Properties loadProperties(String propertiesFileName) {
		Properties prop = new Properties();

		try (InputStream is = getClass().getClassLoader().getResourceAsStream("ontology/" + domain + "/" + propertiesFileName + ".properties")) {
			prop.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return prop;
	}
}
