package xdomainnlp.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import xdomainnlp.namedentityrecognition.TripleSearcher;
import edu.stanford.nlp.ling.CoreLabel;

public class Candidate {
	private Statement candidateStmt;
	private ArrayList<CoreLabel> contextTokens;
	private HashMap<CoreLabel, ArrayList<Statement>> annotationCandidatesByContextToken;
	private TripleSearcher tripleSearcher;
	private ArrayList<CoreLabel> matchedContextTokens;
	private boolean hasMatchingContextToken;

	public Candidate(String domain, Statement candidateStmt, ArrayList<CoreLabel> contextTokens) throws IOException {
		this.candidateStmt = candidateStmt;
		this.contextTokens = contextTokens;
		this.annotationCandidatesByContextToken = new HashMap<>();
		this.tripleSearcher = new TripleSearcher(domain);
		this.matchedContextTokens = new ArrayList<>();
	}

	public void matchCandidateStmtWithAnnotationCandidates() {
		for (Map.Entry<CoreLabel, ArrayList<Statement>> entry : annotationCandidatesByContextToken.entrySet()) {
			for (Statement annotationCandidate : entry.getValue()) {
				if (candidateStmt.getSubject().equals(annotationCandidate.getSubject())) {
					matchedContextTokens.add(entry.getKey());
					hasMatchingContextToken = true;
				}
			}
		}
	}

	public void addAnnotationCandidates(String disambiguationProperty) {
		if (annotationCandidatesByContextToken.isEmpty()) {
			for (CoreLabel contextToken : contextTokens) {
				ArrayList<Statement> annotationCandidates = tripleSearcher.search(null, disambiguationProperty,
						contextToken.get(TextAnnotation.class), 100, 0.9);
				annotationCandidatesByContextToken.put(contextToken, annotationCandidates);
			}
		} else {
			HashMap<CoreLabel, ArrayList<Statement>> prev = annotationCandidatesByContextToken;

			for (CoreLabel contextToken : contextTokens) {
				ArrayList<Statement> prevAnnotationCandidates = prev.get(contextToken);
				ArrayList<Statement> newAnnotationCandidates = tripleSearcher.search(null, disambiguationProperty,
						contextToken.get(TextAnnotation.class), 100, 0.9);
				prevAnnotationCandidates.addAll(newAnnotationCandidates);

				HashSet<Resource> uniqueSubjects = new HashSet<>();
				ArrayList<Statement> uniqueAnnotationCandidates = new ArrayList<>();

				for (Statement stmt : prevAnnotationCandidates) {
					if (!uniqueSubjects.contains(stmt.getSubject())) {
						uniqueSubjects.add(stmt.getSubject());
						uniqueAnnotationCandidates.add(stmt);
					}
				}

				annotationCandidatesByContextToken.put(contextToken, uniqueAnnotationCandidates);
			}
		}
	}

	public Statement getCandidateStmt() {
		return this.candidateStmt;
	}

	public void setCandidateStmt(Statement candidateStmt) {
		this.candidateStmt = candidateStmt;
	}

	public ArrayList<CoreLabel> getContextTokens() {
		return this.contextTokens;
	}

	public void setContextTokens(ArrayList<CoreLabel> contextTokens) {
		this.contextTokens = contextTokens;
	}

	public HashMap<CoreLabel, ArrayList<Statement>> getAnnotationCandidatesByContextToken() {
		return this.annotationCandidatesByContextToken;
	}

	public void setAnnotationCandidatesByContextToken(HashMap<CoreLabel, ArrayList<Statement>> annotationCandidatesByContextToken) {
		this.annotationCandidatesByContextToken = annotationCandidatesByContextToken;
	}

	public ArrayList<CoreLabel> getMatchedContextTokens() {
		return this.matchedContextTokens;
	}

	public void setMatchedContextTokens(ArrayList<CoreLabel> matchedContextTokens) {
		this.matchedContextTokens = matchedContextTokens;
	}

	public boolean hasMatchingContextToken() {
		return this.hasMatchingContextToken;
	}
}