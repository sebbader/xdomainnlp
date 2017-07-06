package xdomainnlp.api;

import java.util.ArrayList;

import org.eclipse.rdf4j.model.Statement;

import rbbnpe.BaseNounPhrase;

public class Entity {
	private int coreNlpSentenceIndex;
	private int coreNlpFirstTokenIndex;
	private int coreNlpLastTokenIndex;
	private String entityString;
	private BaseNounPhrase nounPhrase;
	private boolean isNamedEntity;
	private ArrayList<String> corefSynonyms;
	private Statement labelStmt;
	private String type;

	public Entity() {

	}

	public Entity(String entityString, BaseNounPhrase nounPhrase, boolean isNamedEntity) {
		this.entityString = entityString;
		this.nounPhrase = nounPhrase;
		this.isNamedEntity = isNamedEntity;
		this.corefSynonyms = new ArrayList<>();
	}

	public int getCoreNlpSentenceIndex() {
		return this.coreNlpSentenceIndex;
	}

	public void setCoreNlpSentenceIndex(int coreNlpSentenceIndex) {
		this.coreNlpSentenceIndex = coreNlpSentenceIndex;
	}

	public int getCoreNlpFirstTokenIndex() {
		return this.coreNlpFirstTokenIndex;
	}

	public void setCoreNlpFirstTokenIndex(int coreNlpBeginIndex) {
		this.coreNlpFirstTokenIndex = coreNlpBeginIndex;
	}

	public int getCoreNlpLastTokenIndex() {
		return this.coreNlpLastTokenIndex;
	}

	public void setCoreNlpLastTokenIndex(int coreNlpEndIndex) {
		this.coreNlpLastTokenIndex = coreNlpEndIndex;
	}

	public String getEntityString() {
		return this.entityString;
	}

	public void setEntityString(String entityString) {
		this.entityString = entityString;
	}

	public BaseNounPhrase getNounPhrase() {
		return this.nounPhrase;
	}

	public void setNounPhrase(BaseNounPhrase nounPhrase) {
		this.nounPhrase = nounPhrase;
	}

	public boolean isNamedEntity() {
		return this.isNamedEntity;
	}

	public void setNamedEntity(boolean isNamedEntity) {
		this.isNamedEntity = isNamedEntity;
	}

	public ArrayList<String> getCorefSynonyms() {
		return this.corefSynonyms;
	}

	public void setCorefSynonyms(ArrayList<String> corefSynonyms) {
		this.corefSynonyms = corefSynonyms;
	}

	public void addCorefSynonym(String corefSynonym) {
		this.corefSynonyms.add(corefSynonym);
	}

	public Statement getLabelStmt() {
		return this.labelStmt;
	}

	public void setLabelStmt(Statement labelStmt) {
		this.labelStmt = labelStmt;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
