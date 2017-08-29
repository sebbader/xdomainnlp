package xdomainnlp.namedentityrecognition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import slib.utils.ex.SLIB_Exception;
import xdomainnlp.api.Candidate;
import xdomainnlp.api.Entity;

public class EntityClassificator {
	private String domain;
	private Annotation coreNlpDoc;
	private ArrayList<Entity> entities = new ArrayList<>();
	private TripleSearcher tripleSearcher;
	private Inferencer inferencer;
	private GraphTraverser graphTraverser;

	public EntityClassificator(String domain, Annotation coreNlpDoc, ArrayList<Entity> entities) throws IOException {
		this.domain = domain;
		this.coreNlpDoc = coreNlpDoc;
		this.entities = entities;
		this.tripleSearcher = new TripleSearcher(domain);
		initializeOntology();
	}

	public void classifyEntities(String disambiguationProperty) throws IOException {
		for (Entity entity : entities) {
			if (entity.isNamedEntity()) {
				ArrayList<Statement> candidateStmts = tripleSearcher.search(null, RDFS.LABEL.stringValue(), entity.getNounPhrase().getPhraseString(),
						10, 0.95);

				int numTriedCorefs = 0;
				while (true) {
					// TODO using classes from ontology through lookups in 'ontology-index'
					if (candidateStmts.isEmpty()) {

						/*
						 * Try again with synonyms from coreference resolution
						 */
						if (entity.getCorefSynonyms().size() > numTriedCorefs) {
							candidateStmts = tripleSearcher.search(null, RDFS.LABEL.stringValue(), entity.getCorefSynonyms().get(numTriedCorefs), 10,
									0.9);
							numTriedCorefs += 1; 
							continue;
						}
						break;
					} else if (candidateStmts.size() == 1) {
						if (hasSubclassProperty(candidateStmts.get(0))) {
							entity.setLabelStmt(candidateStmts.get(0));
							entity.setType(graphTraverser.getDirectAncestor(candidateStmts.get(0).getSubject().stringValue()));
						}
						break;
					} else if (candidateStmts.size() > 1) {
						disambiguateEntity(entity, candidateStmts, disambiguationProperty);
						break;
					}
				}
			}
		}

		/*
		 * Infer and set actual type (superclass)
		 */
		for (Entity entity : entities) {
			if (entity.getLabelStmt() != null) {
				entity.setType(graphTraverser.getDirectAncestor(entity.getLabelStmt().getSubject().stringValue()));
			}
		}
	}

	private void disambiguateEntity(Entity entity, ArrayList<Statement> candidateStmts, String disambiguationProperty) throws IOException {
		ArrayList<CoreLabel> contextTokens = getLinguisticContext(entity, 5);
		ArrayList<Candidate> candidates = new ArrayList<>();

		for (Statement candidateStmt : candidateStmts) {
			Candidate candidate = new Candidate(domain, candidateStmt, contextTokens);
			candidate.addAnnotationCandidates(RDFS.COMMENT.stringValue());
			candidate.addAnnotationCandidates(disambiguationProperty);
			candidate.matchCandidateStmtWithAnnotationCandidates();

			if (candidate.hasMatchingContextToken()) {
				candidates.add(candidate);
			}
		}

		if (candidates.size() == 1 && hasSubclassProperty(candidates.get(0).getCandidateStmt())) { // &&
																									// hasSubclassProperty(candidates.get(0).getCandidateStmt())
			entity.setLabelStmt(candidates.get(0).getCandidateStmt());
		} else if (candidates.size() > 1) {
			removeRedundantMatchedContextToken(candidates);
			determineContextualDistances(entity, candidates);
		}
	}

	private void removeRedundantMatchedContextToken(ArrayList<Candidate> candidates) {
		HashSet<CoreLabel> redundantMatchedContextToken = new HashSet<>();

		for (Candidate candidate : candidates) {
			redundantMatchedContextToken.addAll(candidate.getMatchedContextTokens());
		}

		for (Candidate candidate : candidates) {
			redundantMatchedContextToken.retainAll(candidate.getMatchedContextTokens());
		}

		for (Candidate candidate : candidates) {
			candidate.getMatchedContextTokens().removeAll(redundantMatchedContextToken);
		}
	}

	private void determineContextualDistances(Entity entity, ArrayList<Candidate> candidates) {
		CoreLabel closestContextToken = null;
		int currentMinIndex = Integer.MAX_VALUE;
		int currentMinDistance = Integer.MAX_VALUE;

		for (Candidate candidate : candidates) {
			if (!candidate.getMatchedContextTokens().isEmpty()) {
				for (CoreLabel contextToken : candidate.getMatchedContextTokens()) {
					int contextTokenIndex = contextToken.get(IndexAnnotation.class);
					int entityFirstTokenIndex = entity.getCoreNlpFirstTokenIndex();
					int entityLastTokenIndex = entity.getCoreNlpLastTokenIndex();

					if (contextTokenIndex < entityFirstTokenIndex) {
						int distance = Math.abs(contextTokenIndex - entityFirstTokenIndex);

						if (distance < currentMinDistance) {
							currentMinIndex = contextTokenIndex;
						}
					} else if (contextTokenIndex > entityLastTokenIndex) {
						int distance = Math.abs(contextTokenIndex - entityLastTokenIndex);

						if (distance < currentMinDistance) {
							currentMinIndex = contextTokenIndex;
						}
					}
				}
			}
		}

		for (Candidate candidate : candidates) {
			for (CoreLabel contextToken : candidate.getMatchedContextTokens()) {
				if (contextToken.get(IndexAnnotation.class) == currentMinIndex) {
					closestContextToken = contextToken;
				}
			}
		}

		for (Candidate candidate : candidates) {
			if (candidate.getMatchedContextTokens().contains(closestContextToken) && hasSubclassProperty(candidate.getCandidateStmt())) { // &&
																																			// hasSubclassProperty(candidate.getCandidateStmt())
				entity.setLabelStmt(candidate.getCandidateStmt());
			}
		}

		if (entity.getLabelStmt() == null) {
			String commonAncestor = getCommonAncestorBetweenAllCandidates(candidates);
			entity.setType(commonAncestor);
		}
	}

	private String getCommonAncestorBetweenAllCandidates(ArrayList<Candidate> candidates) {
		String commonAncestor = "";

		ArrayList<String> tmp1 = new ArrayList<>();
		ArrayList<String> tmp2 = new ArrayList<>();

		for (int i = 0; i < candidates.size(); i++) {
			tmp1.add(candidates.get(i).getCandidateStmt().getSubject().stringValue());
		}

		try {
			while (true) {
				if (tmp1.size() > 1) {
					for (int i = 0; i < tmp1.size(); i++) {
						for (int j = i + 1; j < tmp1.size(); j++) {
							tmp2.add(graphTraverser.getCommonAncestor(tmp1.get(i), tmp1.get(j)));
						}
					}

					tmp1.clear();
				} else if (tmp1.size() == 1) {
					commonAncestor = tmp1.get(0);
					break;
				}

				if (tmp2.size() > 1) {
					for (int i = 0; i < tmp2.size(); i++) {
						for (int j = i + 1; j < tmp2.size(); j++) {
							tmp1.add(graphTraverser.getCommonAncestor(tmp2.get(i), tmp2.get(j)));
						}
					}

					tmp2.clear();
				} else if (tmp2.size() == 1) {
					commonAncestor = tmp2.get(0);
					break;
				}
			}
		} catch (SLIB_Exception e) {
			e.printStackTrace();
		}

		return commonAncestor;
	}

	private ArrayList<CoreLabel> getLinguisticContext(Entity entity, int contextSize) {
		setEntityCoreNlpIndices(entity);

		CoreMap coreNlpSentence = coreNlpDoc.get(SentencesAnnotation.class).get(entity.getCoreNlpSentenceIndex());
		List<CoreLabel> coreNlpTokens = coreNlpSentence.get(TokensAnnotation.class);

		int entityFirstTokenIndex = entity.getCoreNlpFirstTokenIndex();
		int entityLastTokenIndex = entity.getCoreNlpLastTokenIndex();
		int contextBegin = entityFirstTokenIndex - contextSize;
		int contextEnd = entityLastTokenIndex + contextSize;
		List<CoreLabel> leftContext = new ArrayList<>();
		List<CoreLabel> rightContext = new ArrayList<>();

		if (contextBegin >= 1 && contextEnd <= coreNlpTokens.size()) {
			leftContext = coreNlpTokens.subList(contextBegin - 1, entityFirstTokenIndex - 1);
			rightContext = coreNlpTokens.subList(entityLastTokenIndex, contextEnd);
		} else if (contextBegin >= 1 && contextEnd > coreNlpTokens.size()) {
			leftContext = coreNlpTokens.subList(contextBegin - 1, entityFirstTokenIndex - 1);
			rightContext = coreNlpTokens.subList(entityLastTokenIndex, coreNlpTokens.size());
		} else if (contextBegin < 1 && contextEnd <= coreNlpTokens.size()) {
			leftContext = coreNlpTokens.subList(0, entityFirstTokenIndex - 1);
			rightContext = coreNlpTokens.subList(entityLastTokenIndex, contextEnd);
		} else if (contextBegin < 1 && contextEnd > coreNlpTokens.size()) {
			leftContext = coreNlpTokens.subList(0, entityFirstTokenIndex - 1);
			rightContext = coreNlpTokens.subList(entityLastTokenIndex, coreNlpTokens.size());
		}

		return filterContextTokens(leftContext, rightContext);
	}

	private ArrayList<CoreLabel> filterContextTokens(List<CoreLabel> leftContext, List<CoreLabel> rightContext) {
		ArrayList<CoreLabel> result = new ArrayList<>();

		for (CoreLabel token : leftContext) {
			if (token.get(PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS")) {
				result.add(token);
			}
		}

		for (CoreLabel token : rightContext) {
			if (token.get(PartOfSpeechAnnotation.class).matches("NN|NNS|NNP|NNPS")) {
				result.add(token);
			}
		}

		return result;
	}

	private void setEntityCoreNlpIndices(Entity entity) {
		int entityBeginOffset = entity.getNounPhrase().getStartOffset();
		int entityEndOffset = entityBeginOffset + entity.getNounPhrase().getPhraseString().length();

		for (CoreMap sentence : coreNlpDoc.get(SentencesAnnotation.class)) {
			int sentenceIndex = sentence.get(SentenceIndexAnnotation.class);

			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				int tokenBeginOffset = token.get(CharacterOffsetBeginAnnotation.class);
				int tokenEndOffset = token.get(CharacterOffsetEndAnnotation.class);
				int tokenIndex = token.get(IndexAnnotation.class);

				if (entityBeginOffset == tokenBeginOffset) {
					entity.setCoreNlpFirstTokenIndex(tokenIndex);
					entity.setCoreNlpSentenceIndex(sentenceIndex);
				}

				if (entityEndOffset == tokenEndOffset) {
					entity.setCoreNlpLastTokenIndex(tokenIndex);
				}
			}
		}
	}

	private boolean hasSubclassProperty(Statement labelStmt) {
		ArrayList<Statement> correspondingStmts = tripleSearcher.search(labelStmt.getSubject().stringValue(), null, null, 100, 1.0);
		boolean hasSubclassProperty = false;

		for (Statement stmt : correspondingStmts) {
			if (stmt.getPredicate().equals(RDFS.SUBCLASSOF)) {
				hasSubclassProperty = true;
			}
		}

		return hasSubclassProperty;
	}

	private void initializeOntology() {
		Properties ontologyProps = loadProperties(AnnotationMerger.PROPERTIES_FILE_ONTOLOGY);
		String ontologyBaseUri = ontologyProps.getProperty("ontologyBaseUri");
		File inferredOntology = new File("src/main/resources/ontology/" + domain + "/" + AnnotationMerger.PROPERTIES_FILE_INFERRED_ONTOLOGY + ".ttl");
		inferencer = new Inferencer(domain, ontologyBaseUri, "select", "SELECT ?s ?p ?o WHERE { ?s ?p ?o . }");

		if (!inferredOntology.exists()) {
			inferencer.inferNewStmts();
		}

		try {
			graphTraverser = new GraphTraverser(domain, ontologyBaseUri);
		} catch (SLIB_Exception e) {
			e.printStackTrace();
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
