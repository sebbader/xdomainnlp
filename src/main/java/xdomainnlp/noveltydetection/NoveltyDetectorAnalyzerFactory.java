package xdomainnlp.noveltydetection;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.Version;

public class NoveltyDetectorAnalyzerFactory {

	final List<String> allStopWords = Arrays.asList("'ll", "a", "able", "about", "above", "abst", "accordance", "according", "accordingly", "across",
			"act", "actually", "added", "adj", "affected", "affecting", "affects", "after", "afterwards", "again", "against", "ah", "all", "allows",
			"almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "announce", "another", "any",
			"anybody", "anyhow", "anymore", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "apparently", "appear", "appropriate",
			"approximately", "are", "aren", "aren't", "arent", "arise", "around", "as", "aside", "ask", "asking", "associated", "at", "auth",
			"available", "away", "awfully", "b", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand",
			"begin", "beginning", "beginnings", "begins", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between",
			"beyond", "biol", "both", "brief", "briefly", "but", "by", "ca", "came", "can", "can't", "cannot", "cant", "cause", "causes", "certain",
			"certainly", "changes", "co", "com", "come", "comes", "consequently", "contain", "containing", "contains", "corresponding", "could",
			"couldn", "couldn't", "couldnt", "currently", "d", "date", "day", "described", "did", "didn", "didn't", "didnt", "different", "do",
			"does", "doesn", "doesn't", "doesnt", "doing", "don", "don't", "done", "dont", "down", "downwards", "due", "during", "e", "each", "ed",
			"edu", "effect", "eg", "eight", "eighty", "either", "else", "elsewhere", "end", "ending", "enough", "especially", "et", "et-al", "etc",
			"even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "example", "except", "f", "far", "few", "ff", "fifth",
			"first", "five", "fix", "followed", "following", "follows", "for", "former", "formerly", "forth", "found", "four", "from", "further",
			"furthermore", "g", "gave", "get", "get's", "gets", "getting", "give", "given", "gives", "giving", "go", "goes", "gone", "good", "got",
			"gotten", "great", "gt", "h", "had", "happens", "hardly", "has", "hasn", "hasn't", "hasnt", "have", "haven", "haven't", "havent",
			"having", "havnt", "he", "he's", "hed", "hence", "her", "here", "hereafter", "hereby", "herein", "heres", "hereupon", "hers", "herself",
			"hes", "hi", "hid", "him", "himself", "his", "hither", "home", "how", "howbeit", "however", "hundred", "i", "i'd", "i'll", "i'm", "i've",
			"id", "ie", "if", "ignored", "ill", "im", "immediate", "immediately", "importance", "important", "in", "inasmuch", "inc", "indeed",
			"indicate", "indicated", "indicates", "information", "inner", "insofar", "instead", "into", "invention", "inward", "is", "isn", "isn't",
			"isnt", "it", "it'd", "it'll", "it's", "itd", "itll", "its", "itself", "ive", "j", "just", "k", "keep", "keeps", "kept", "kg", "km",
			"know", "known", "knows", "l", "largely", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "let's",
			"lets", "life", "like", "liked", "likely", "line", "little", "ll", "long", "look", "looking", "looks", "lt", "ltd", "m", "made", "mainly",
			"make", "makes", "man", "many", "may", "maybe", "me", "mean", "means", "meantime", "meanwhile", "men", "merely", "mg", "might", "million",
			"miss", "ml", "more", "moreover", "most", "mostly", "mr", "mrs", "much", "mug", "must", "my", "myself", "n", "na", "name", "namely",
			"nay", "nd", "near", "nearly", "necessarily", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine",
			"ninety", "no", "nobody", "non", "none", "nonetheless", "noone", "nor", "normally", "nos", "not", "noted", "nothing", "novel", "now",
			"nowhere", "o", "obtain", "obtained", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "omitted", "on", "once", "one",
			"ones", "only", "onto", "or", "ord", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over",
			"overall", "owing", "own", "p", "page", "pages", "part", "particular", "particularly", "past", "people", "per", "perhaps", "placed",
			"please", "plus", "poorly", "possible", "possibly", "potentially", "pp", "predominantly", "present", "previously", "primarily",
			"probably", "promptly", "proud", "provides", "put", "q", "que", "quickly", "quite", "qv", "r", "ran", "rather", "rd", "re", "readily",
			"really", "recent", "recently", "ref", "refs", "regarding", "regardless", "regards", "related", "relatively", "research", "respectively",
			"resulted", "resulting", "results", "right", "run", "s", "said", "same", "saw", "say", "saying", "says", "sec", "second", "secondly",
			"section", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seven",
			"several", "shall", "she", "she'd", "she'll", "she's", "shed", "shes", "should", "shouldn", "shouldn't", "shouldnt", "show", "showed",
			"shown", "showns", "shows", "significant", "significantly", "similar", "similarly", "since", "six", "slightly", "so", "some", "somebody",
			"somehow", "someone", "somethan", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specifically",
			"specified", "specify", "specifying", "state", "still", "stop", "strongly", "sub", "substantially", "successfully", "such",
			"sufficiently", "suggest", "sup", "sure", "t", "take", "taken", "than", "that", "that's", "thats", "the", "their", "their's", "theirs",
			"them", "themselves", "then", "thence", "there", "there's", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon",
			"these", "they", "they'd", "they'll", "they're", "they've", "theyd", "theyll", "theyre", "theyve", "third", "this", "thorough",
			"thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "time", "to", "together", "too", "toward", "towards",
			"twice", "two", "u", "under", "unless", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "v",
			"value", "various", "ve", "very", "via", "viz", "vs", "w", "was", "wasn", "wasn't", "wasnt", "way", "we", "we'd", "we'll", "we're",
			"we've", "wed", "well", "went", "were", "weren", "weren't", "werent", "weve", "what", "what's", "whatever", "whats", "when", "when's",
			"whence", "whenever", "whens", "where", "where's", "whereafter", "whereas", "whereby", "wherein", "wheres", "whereupon", "wherever",
			"whether", "which", "while", "whither", "who", "who's", "whoever", "whole", "whom", "whos", "whose", "why", "why's", "whys", "will",
			"with", "within", "without", "won", "won't", "wont", "work", "world", "would", "wouldn", "wouldn't", "wouldnt", "x", "y", "year", "years",
			"yet", "you", "you'd", "you'll", "you're", "you've", "youd", "youll", "your", "youre", "yours", "yourself", "yourselves", "youve", "z",
			"zero");

	final List<String> apostropheStopWords = Arrays.asList("'ll", "am", "aren't", "can't", "couldn't", "didn't", "doesn't", "don't", "get's",
			"hasn't", "haven't", "he's", "i'd", "i'll", "i'm", "i've", "isn't", "it'd", "it'll", "it's", "let's", "she'd", "she'll", "she's",
			"shouldn't", "that's", "their's", "there's", "they'd", "they'll", "they're", "they've", "wasn't", "we'd", "we'll", "we're", "we've",
			"weren't", "what's", "when's", "where's", "who's", "why's", "won't", "wouldn't", "you'd", "you'll", "you're", "you've");

	final static List<String> abbreviationStopWords = Arrays.asList("i.e.", "i. e.", "i.e", "ie.", "e.g.", "e. g.", "e.g", "eg.", "p.s.", "p. s.",
			"p.s", "ca.", "c.a.", "c. a.", "c.a", "etc.", "pp.", "al.", "mr.", "mrs.");

	final CharArraySet allStopWordsSet = new CharArraySet(Version.LUCENE_44, allStopWords, true);
	final CharArraySet apostropheStopWordsSet = new CharArraySet(Version.LUCENE_44, apostropheStopWords, true);
	final static CharArraySet abbreviationStopWordsSet = new CharArraySet(Version.LUCENE_44, abbreviationStopWords, true);

	public static Analyzer createTopicAnalyzerPre() {
		Analyzer bodyAnalyzer = new Analyzer() {

			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new LengthFilter(Version.LUCENE_44, tokenizer, 1, 128);
				filter = new LowerCaseFilter(Version.LUCENE_44, filter);
				filter = new EnglishPossessiveFilter(Version.LUCENE_44, filter);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return bodyAnalyzer;
	}

	public static Analyzer createTopicAnalyzerRegex() {
		Analyzer bodyAnalyzer = new Analyzer() {

			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new TopicCharTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new PatternReplaceFilter(tokenizer, Pattern.compile("'"), "", true);
				filter = new StopFilter(Version.LUCENE_44, filter, abbreviationStopWordsSet);
				filter = new PatternReplaceFilter(filter, Pattern.compile("\\.{2,}|\\.$|^\\++|((?<!\\+)\\+$|\\+{3,}$)|^_+|_+$|^#+|#{2,}"), "", true);
				filter = new PatternReplaceFilter(filter, Pattern.compile("\\b\\++\\b|\\b#+\\b"), " ", true);
				filter = new LengthFilter(Version.LUCENE_44, filter, 1, 128);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return bodyAnalyzer;
	}

	public static Analyzer createTopicAnalyzerStem() {
		Analyzer bodyAnalyzer = new Analyzer() {

			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new EnglishMinimalStemFilter(tokenizer);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return bodyAnalyzer;
	}

	public static Analyzer createTitleIndexAnalyzer() {
		Analyzer testAnalyzer = new Analyzer() {

			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new PostCharTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new EnglishPossessiveFilter(Version.LUCENE_44, tokenizer);
				filter = new SynonymFilter(filter, createSynonymMap(), true);
				filter = new StopFilter(Version.LUCENE_44, filter, abbreviationStopWordsSet);
				filter = new PatternReplaceFilter(filter, Pattern.compile("\\.{2,}|\\.$|^\\++|((?<!\\+)\\+$|\\+{3,}$)|^_+|_+$|^#+|#{2,}|'|^-+|-+$"),
						"", true);
				filter = new WordDelimiterFilter(filter, WordDelimiterFilter.CATENATE_WORDS | WordDelimiterFilter.GENERATE_WORD_PARTS
						| WordDelimiterFilter.PRESERVE_ORIGINAL | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE | WordDelimiterFilter.SPLIT_ON_NUMERICS,
						null);
				filter = new RemoveDuplicatesTokenFilter(filter);
				filter = new LowerCaseFilter(Version.LUCENE_44, filter);
				filter = new StopFilter(Version.LUCENE_44, filter, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
				filter = new LengthFilter(Version.LUCENE_44, filter, 1, 512);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return testAnalyzer;
	}

	public static Analyzer createBodyIndexAnalyzer() {
		Analyzer testAnalyzer = new Analyzer() {

			@Override
			protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				Tokenizer tokenizer = new PostCharTokenizer(Version.LUCENE_44, reader);
				TokenStream filter = new EnglishPossessiveFilter(Version.LUCENE_44, tokenizer);
				filter = new SynonymFilter(filter, createSynonymMap(), true);
				filter = new StopFilter(Version.LUCENE_44, filter, abbreviationStopWordsSet);
				filter = new PatternReplaceFilter(filter, Pattern.compile("\\.{2,}|\\.$|^\\++|((?<!\\+)\\+$|\\+{3,}$)|^_+|_+$|^#+|#{2,}|'|^-+|-+$"),
						"", true);
				filter = new WordDelimiterFilter(filter, WordDelimiterFilter.CATENATE_WORDS | WordDelimiterFilter.GENERATE_WORD_PARTS
						| WordDelimiterFilter.PRESERVE_ORIGINAL | WordDelimiterFilter.SPLIT_ON_CASE_CHANGE | WordDelimiterFilter.SPLIT_ON_NUMERICS,
						null);
				filter = new RemoveDuplicatesTokenFilter(filter);
				filter = new LowerCaseFilter(Version.LUCENE_44, filter);
				filter = new StopFilter(Version.LUCENE_44, filter, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
				filter = new LengthFilter(Version.LUCENE_44, filter, 1, 512);

				return new TokenStreamComponents(tokenizer, filter);
			}
		};

		return testAnalyzer;
	}

	public static SynonymMap createSynonymMap() {
		SynonymMap.Builder builder = new SynonymMap.Builder(true);

		builder.add(new CharsRef("c++"), new CharsRef("cplusplus"), false);
		builder.add(new CharsRef("c#"), new CharsRef("csharp"), false);
		builder.add(new CharsRef(".net"), new CharsRef("dotnet"), false);
		builder.add(new CharsRef("js"), new CharsRef("javascript"), false);

		SynonymMap synonymMap = null;
		try {
			synonymMap = builder.build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return synonymMap;
	}

	public static String applyAnalyzer(String text, Analyzer analyzer) throws IOException {
		StringJoiner sj = new StringJoiner(" ");

		TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

		tokenStream.reset();

		while (tokenStream.incrementToken()) {
			sj.add(charTermAttribute.toString());
		}

		tokenStream.end();
		tokenStream.close();

		return sj.toString();
	}
}
