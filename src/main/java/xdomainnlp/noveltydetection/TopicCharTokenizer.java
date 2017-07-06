package xdomainnlp.noveltydetection;

import java.io.Reader;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;

public class TopicCharTokenizer extends CharTokenizer {

	public TopicCharTokenizer(Version matchVersion, Reader input) {
		super(matchVersion, input);
	}

	@Override
	protected boolean isTokenChar(int c) {

		return Character.isLetter(c) || c == ".".charAt(0) || c == "_".charAt(0) || c == "#".charAt(0) || c == "'".charAt(0) || c == "+".charAt(0);
	}
}
