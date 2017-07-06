package xdomainnlp.noveltydetection;

import cc.mallet.types.*;
import xdomainnlp.api.Post;
import xdomainnlp.noveltydetection.utilities.TopicsExtractor;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class TopicModeler {

	public static void trainModel(String inputFilePath, String stoplistPath) throws IOException {

		// Import documents from text to feature sequences.
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Lowercase, tokenize, remove stopwords, map to features.
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("[\\p{L}\\p{P}]*[\\p{L}|\\p{P}|\\p{S}]+")));
		pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplistPath"), "UTF-8", true, false, false));
		pipeList.add(new TokenSequence2FeatureSequence());

		InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		Reader fileReader = new InputStreamReader(new FileInputStream(new File(inputFilePath)), "UTF-8");
		instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, label, name fields

		// Serialize instances.
		serializeInstances(instances);

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01.
		int numTopics = 160;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 5.0, 0.01);

		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine statistics after every iteration.
		model.setNumThreads(2);

		// Run the model for 1200 iterations and stop.
		model.setNumIterations(1200);

		// Set optimize interval.
		model.setOptimizeInterval(40);

		// Set burn in.
		model.setBurninPeriod(80);

		// Run training.
		model.estimate();

		// The data alphabet maps word IDs to strings.
		Alphabet dataAlphabet = instances.getDataAlphabet();

		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
		LabelSequence topics = model.getData().get(0).topicSequence;

		Formatter out = new Formatter(new StringBuilder(), Locale.US);

		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}

		System.out.println(out);

		// Estimate the topic distribution of the first instance.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs.
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Show top 5 words in topics with proportions for the first document.
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;

			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}

			System.out.println(out);
		}

		// Create a new instance with high probability of topic 0.
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
			rank++;
		}

		// Serialize model.
		serializeModel(model);

		// Serialize output state.
		serializeOutputState(model);

		// Serialize inferencer.
		serializeInferencer(model);

		// Serialize evaluator.
		serializeEvaluator(model);

		// Serialize output topic keys.
		serializeOutputTopicKeys(model);

		// Serialize output doc topics.
		serializeOutputDocTopics(model);
	}

	private static void serializeInstances(InstanceList instances) throws FileNotFoundException, IOException {
		ObjectOutputStream oos;
		oos = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + "/lda/instances.ser"));
		oos.writeObject(instances);
		oos.close();
	}

	private static void serializeModel(ParallelTopicModel model) {
		assert (model != null);
		try {

			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + "/lda/model.ser"));
			oos.writeObject(model);
			oos.close();

		} catch (Exception e) {
			System.out.println("Couldn't write topic model");
		}
	}

	private static void serializeOutputState(ParallelTopicModel model) throws IOException {
		model.printState(new File(System.getProperty("user.home") + "/lda/output-state.gz"));
	}

	private static void serializeInferencer(ParallelTopicModel model) throws IOException {
		try {

			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + "/lda/inferencer.ser"));
			oos.writeObject(model.getInferencer());
			oos.close();

		} catch (Exception e) {
			System.out.println("Couldn't create inferencer: " + e.getMessage());
		}
	}

	private static void serializeEvaluator(ParallelTopicModel model) {
		try {

			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + "/lda/evaluator.ser"));
			oos.writeObject(model.getProbEstimator());
			oos.close();

		} catch (Exception e) {
			System.out.println("Couldn't create evaluator: " + e.getMessage());
		}
	}

	private static void serializeOutputTopicKeys(ParallelTopicModel model) throws IOException {
		model.printTopWords(new File(System.getProperty("user.home") + "/lda/topic-keys.txt"), 20, false);
	}

	private static void serializeOutputDocTopics(ParallelTopicModel model) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter((new File(System.getProperty("user.home") + "/lda/doc-topics.txt"))));
		model.printDenseDocumentTopics(out);
		out.close();
	}

	public static void serializePipes() {
		InstanceList instanceList = InstanceList.load(new File(System.getProperty("user.home") + "/lda/instances.ser"));
		Pipe pipe = instanceList.getPipe();

		try {
			FileOutputStream outFile = new FileOutputStream(System.getProperty("user.home") + "/lda/pipe.ser");
			ObjectOutputStream oos = new ObjectOutputStream(outFile);
			oos.writeObject(pipe);
			oos.close();
		} catch (FileNotFoundException ex) {
			System.out.println(ex.getMessage());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	public static void InferByModel(String sentence) {
		// Define model and pipeline.
		ParallelTopicModel model = null;
		SerialPipes pipes = null;

		// Load model.
		try {
			FileInputStream inFile = new FileInputStream(System.getProperty("user.home") + "/lda/model.ser");
			ObjectInputStream ois = new ObjectInputStream(inFile);
			model = (ParallelTopicModel) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			System.out.println("Could not read model from file: " + ex);
		} catch (ClassNotFoundException ex) {
			System.out.println("Could not load the model: " + ex);
		}

		// Load pipeline.
		try {
			FileInputStream inFile = new FileInputStream(System.getProperty("user.home") + "/lda/pipe.ser");
			ObjectInputStream ois = new ObjectInputStream(inFile);
			pipes = (SerialPipes) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			System.out.println("Could not read pipes from file: " + ex);
		} catch (ClassNotFoundException ex) {
			System.out.println("Could not load the pipes: " + ex);
		}

		if (model != null && pipes != null) {

			// Create new instance.
			InstanceList testing = new InstanceList(pipes);
			testing.addThruPipe(new Instance(sentence, null, "input", null));

			// Load inferencer from model.
			TopicInferencer inferencer = model.getInferencer();
			double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < testProbabilities.length; i++) {
				sb.append(testProbabilities[i] + " ");
			}
		}
	}

	public static void inferTopicDistribution(Post inputDoc) throws IOException {
		String domain = inputDoc.getDomain();
		String inputText = TopicsExtractor.runAnalyzerChain(inputDoc.getTitle() + " " + inputDoc.getBody());
		TopicInferencer inferencer = null;
		SerialPipes pipes = null;

		// Load inferencer.
		try {
			FileInputStream inFile = new FileInputStream("src/main/resources/mallet-models/" + domain + "/inferencer.ser");
			ObjectInputStream ois = new ObjectInputStream(inFile);
			inferencer = (TopicInferencer) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			System.out.println("Could not read inferencer from file: " + ex);
		} catch (ClassNotFoundException ex) {
			System.out.println("Could not load the inferencer: " + ex);
		}

		// Load pipeline.
		try {
			FileInputStream inFile = new FileInputStream("src/main/resources/mallet-models/" + domain + "/pipe.ser");
			ObjectInputStream ois = new ObjectInputStream(inFile);
			pipes = (SerialPipes) ois.readObject();
			ois.close();
		} catch (IOException ex) {
			System.out.println("Could not read pipes from file: " + ex);
		} catch (ClassNotFoundException ex) {
			System.out.println("Could not load the pipes: " + ex);
		}

		if (inferencer != null && pipes != null) {
			InstanceList instanceList = new InstanceList(pipes);
			instanceList.addThruPipe(new Instance(inputText, null, "input", null));
			inputDoc.setTopicWeights(inferencer.getSampledDistribution(instanceList.get(0), 10, 1, 5));
		} else {
			throw new NullPointerException("Inferencer and pipes must not be null.");
		}
	}
}
