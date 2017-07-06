package xdomainnlp.interfaces;

import org.eclipse.rdf4j.model.Model;

public interface NifToken {
	void setNextToken(NifToken nextToken);

	int getTokenIndex();

	String getTokenText();

	int getTokenBeginOffset();

	int getTokenEndOffset();

	Model createRDFModel(String serviceBaseUri, String domain, String mongoDocId);
}
