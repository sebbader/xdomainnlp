package xdomainnlp.noveltydetection.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import xdomainnlp.noveltydetection.NoveltyDetectorAnalyzerFactory;

public class TopicsExtractor {

	private String inputfile = "C:\\so-index\\posts_short.txt";
	private String outputfile = "C:\\so-index\\posts_short_topics-training.txt";

	private boolean lineIncomplete = true;
	private String id = "";
	private String title = "";
	private String body = "";

	public void extract() throws FileNotFoundException, IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile), "utf-8"));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile), "utf-8"));

			String line = "";

			int count = 0;

			while ((line = reader.readLine()) != null) {
				splitLine(line);

				if (lineIncomplete) {
					continue;
				}

				title = runAnalyzerChain(title);
				body = runAnalyzerChain(body);

				writer.append(id + "\tX\t");
				writer.append(title + " ");
				writer.append(body + "\n");

				writer.toString();

				writer.flush();

				++count;

				if (count % 10000 == 0) {
					System.out.println(count + " lines processed.");
				}
			}

			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void splitLine(String line) {
		String[] lineParts = line.split("\t");

		id = "";
		title = "";
		body = "";

		if (lineParts.length < 4) {
			lineIncomplete = true;
		} else if (lineParts.length == 4) {
			lineIncomplete = false;
			id = lineParts[0];
			title = lineParts[1];
			body = lineParts[2];
		}
	}

	public static String removeStopwords(String text) {
		List<String> stopWords = Arrays.asList("'ll", "a", "able", "about", "above", "abst", "according", "accordingly", "across", "actually",
				"added", "adj", "affected", "affecting", "affects", "after", "afterwards", "again", "against", "ah", "all", "almost", "alone",
				"along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "announce", "another", "any", "anybody",
				"anyhow", "anymore", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "apparently", "appear", "appropriate",
				"approximately", "are", "aren", "aren't", "arent", "arise", "around", "as", "aside", "ask", "asking", "associated", "at", "auth",
				"available", "away", "awfully", "b", "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before",
				"beforehand", "begins", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "biol",
				"both", "brief", "briefly", "but", "by", "ca", "came", "can", "can't", "cannot", "cant", "cause", "causes", "certain", "certainly",
				"co", "com", "come", "comes", "consequently", "corresponding", "could", "couldn", "couldn't", "couldnt", "currently", "d",
				"described", "did", "didn", "didn't", "didnt", "different", "do", "does", "doesn", "doesn't", "doesnt", "doing", "don", "don't",
				"done", "dont", "down", "downwards", "due", "during", "e", "each", "ed", "edu", "eg", "eight", "eighty", "either", "else",
				"elsewhere", "enough", "especially", "et", "et-al", "etc", "even", "ever", "every", "everybody", "everyone", "everything",
				"everywhere", "ex", "example", "except", "f", "far", "few", "ff", "fifth", "first", "five", "fix", "followed", "following", "follows",
				"for", "former", "formerly", "forth", "found", "four", "from", "further", "furthermore", "g", "gave", "get", "get's", "gets",
				"getting", "give", "given", "gives", "giving", "go", "goes", "gone", "good", "got", "gotten", "great", "gt", "h", "had", "happens",
				"hardly", "has", "hasn", "hasn't", "hasnt", "have", "haven", "haven't", "havent", "having", "havnt", "he", "he's", "hed", "hence",
				"her", "here", "hereafter", "hereby", "herein", "heres", "hereupon", "hers", "herself", "hes", "hi", "hid", "him", "himself", "his",
				"hither", "how", "howbeit", "however", "hundred", "i", "i'd", "i'll", "i'm", "i've", "iam", "id", "ie", "if", "ill", "im",
				"immediate", "immediately", "importance", "important", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates",
				"inner", "insofar", "instead", "into", "inward", "is", "isn", "isn't", "isnt", "it", "it'd", "it'll", "it's", "itd", "itll", "its",
				"itself", "ive", "j", "just", "k", "keep", "keeps", "kept", "kg", "km", "know", "known", "knows", "l", "largely", "last", "lately",
				"later", "latter", "latterly", "least", "less", "lest", "let", "let's", "lets", "like", "liked", "likely", "little", "ll", "look",
				"looking", "looks", "lt", "ltd", "m", "made", "mainly", "makes", "man", "many", "may", "maybe", "me", "mean", "means", "meantime",
				"meanwhile", "men", "merely", "mg", "might", "million", "miss", "ml", "more", "moreover", "most", "mostly", "mr", "mrs", "much",
				"must", "my", "myself", "n", "na", "namely", "nay", "nd", "near", "nearly", "necessarily", "necessary", "need", "needs", "neither",
				"never", "nevertheless", "new", "next", "nine", "ninety", "no", "nobody", "non", "none", "nonetheless", "noone", "nor", "normally",
				"nos", "not", "noted", "nothing", "novel", "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay", "old",
				"omitted", "on", "once", "one", "ones", "only", "onto", "or", "ord", "other", "others", "otherwise", "ought", "our", "ours",
				"ourselves", "out", "outside", "over", "overall", "owing", "own", "p", "particular", "particularly", "past", "per", "perhaps",
				"placed", "please", "poorly", "possibly", "potentially", "pp", "predominantly", "present", "previously", "primarily", "probably",
				"promptly", "proud", "q", "que", "quickly", "quite", "qv", "r", "ran", "rather", "rd", "re", "readily", "really", "recent",
				"recently", "regarding", "regardless", "regards", "related", "relatively", "respectively", "resulted", "resulting", "right", "s",
				"said", "same", "saw", "say", "saying", "says", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self",
				"selves", "sensible", "sent", "serious", "seven", "several", "shall", "she", "she'd", "she'll", "she's", "shed", "shes", "should",
				"shouldn", "shouldn't", "shouldnt", "showed", "shown", "showns", "shows", "significant", "significantly", "similar", "similarly",
				"since", "six", "slightly", "so", "some", "somebody", "somehow", "someone", "somethan", "something", "sometime", "sometimes",
				"somewhat", "somewhere", "soon", "sorry", "specifically", "specified", "specify", "specifying", "still", "strongly", "sub",
				"substantially", "successfully", "such", "sufficiently", "suggest", "sup", "sure", "t", "take", "taken", "than", "thank", "thanks",
				"thankful", "that", "that's", "thats", "the", "their", "their's", "theirs", "them", "themselves", "then", "thence", "there",
				"there's", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "they'd", "they'll", "they're",
				"they've", "theyd", "theyll", "theyre", "theyve", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through",
				"throughout", "thru", "thus", "to", "together", "too", "toward", "towards", "twice", "two", "u", "under", "unless", "until", "unto",
				"up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "v", "various", "ve", "very", "via", "viz", "vs", "w", "was",
				"wasn", "wasn't", "wasnt", "way", "we", "we'd", "we'll", "we're", "we've", "wed", "well", "went", "were", "weren", "weren't",
				"werent", "weve", "what", "what's", "whatever", "whats", "when", "when's", "whence", "whenever", "whens", "where", "where's",
				"whereafter", "whereas", "whereby", "wherein", "wheres", "whereupon", "wherever", "whether", "which", "while", "whither", "who",
				"who's", "whoever", "whole", "whom", "whos", "whose", "why", "why's", "whys", "will", "with", "within", "without", "won", "won't",
				"wont", "work", "world", "would", "wouldn", "wouldn't", "wouldnt", "x", "y", "yet", "you", "you'd", "you'll", "you're", "you've",
				"youd", "youll", "your", "youre", "yours", "yourself", "yourselves", "youve", "z");

		List<String> tokens = new ArrayList<String>(Arrays.asList(text.split(" ")));

		tokens.removeAll(stopWords);

		String result = StringUtils.join(tokens, " ");

		return result;
	}

	public static String runAnalyzerChain(String text) throws IOException {
		String result = text;

		result = NoveltyDetectorAnalyzerFactory.applyAnalyzer(result, NoveltyDetectorAnalyzerFactory.createTopicAnalyzerPre());
		result = NoveltyDetectorAnalyzerFactory.applyAnalyzer(result, NoveltyDetectorAnalyzerFactory.createTopicAnalyzerRegex());
		result = removeStopwords(result);
		result = NoveltyDetectorAnalyzerFactory.applyAnalyzer(result, NoveltyDetectorAnalyzerFactory.createTopicAnalyzerStem());

		return result;
	}
}
