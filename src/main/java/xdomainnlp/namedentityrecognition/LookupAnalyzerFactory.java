package xdomainnlp.namedentityrecognition;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import xdomainnlp.api.NgramAnalysisResult;

public class LookupAnalyzerFactory {

	public static Analyzer createNGramAnalyzer(int minGrams, int maxGrams) {
		Analyzer nGramAnalyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new NGramTokenizer(Version.LUCENE_44, reader, minGrams, maxGrams);
				TokenStream filter = new LowerCaseFilter(Version.LUCENE_44, tokenizer);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return nGramAnalyzer;
	}

	public static Analyzer createShingleAnalyzer(int minShingles, int maxShingles) {
		Analyzer shingleAnalyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new LowerCaseFilter(Version.LUCENE_44, tokenizer);
				if (minShingles > 0) {
					filter = new ShingleFilter(filter, minShingles, maxShingles);
				}

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return shingleAnalyzer;
	}

	public static Analyzer createLowercaseWhitespaceAnalyzer() {
		Analyzer lowercaseWhitespaceAnalyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new LowerCaseFilter(Version.LUCENE_44, tokenizer);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return lowercaseWhitespaceAnalyzer;
	}

	public static Analyzer createLiteralAnalyzer() {
		Analyzer literalAnalyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new LowerCaseTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new ASCIIFoldingFilter(tokenizer);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return literalAnalyzer;
	}

	public static Analyzer createUrlAnalyzer() {
		Analyzer urlAnalyzer = new SimpleAnalyzer(Version.LUCENE_44);

		return urlAnalyzer;
	}

	public static List<String> applyAnalyzer(String searchItem, Analyzer analyzer) throws IOException {
		TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(searchItem));
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

		List<String> searchTerms = new ArrayList<String>();

		tokenStream.reset();

		while (tokenStream.incrementToken()) {
			searchTerms.add(charTermAttribute.toString());
		}

		tokenStream.end();
		tokenStream.close();

		return searchTerms;
	}

	public static NgramAnalysisResult analyzeNgrams(String input) throws IOException {
		NgramAnalysisResult result = new NgramAnalysisResult();
		result.input = input.toLowerCase();
		Analyzer analyzer = createNGramAnalyzer(2, 4);
		TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(input));
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

		tokenStream.reset();

		while (tokenStream.incrementToken()) {
			String text = charTermAttribute.toString();

			if (text.length() == 2) {
				result.gram2s.add(text);
			} else if (text.length() == 3) {
				result.gram3s.add(text);
			} else if (text.length() == 4) {
				result.gram4s.add(text);
			} else {
				continue;
			}
		}

		result.gramBeginEdge3 = input.substring(0, Math.min(input.length(), 3)).toLowerCase();
		result.gramBeginEdge4 = input.substring(0, Math.min(input.length(), 4)).toLowerCase();
		result.gramEndEdge3 = input.substring(Math.max(0, input.length() - 3), input.length()).toLowerCase();
		result.gramEndEdge4 = input.substring(Math.max(0, input.length() - 4), input.length()).toLowerCase();

		return result;
	}
}
