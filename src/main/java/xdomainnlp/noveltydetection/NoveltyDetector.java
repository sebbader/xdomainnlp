package xdomainnlp.noveltydetection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import xdomainnlp.api.Post;

public class NoveltyDetector {

	private Post inputDoc;
	private String domain;
	private IndexReader ir;
	private IndexSearcher is;
	private final Map<Integer, Post> preselectedPosts = new LinkedHashMap<>();
	private final Set<String> terms = new HashSet<>();

	public NoveltyDetector(Post inputDoc, int numComparedDocs) throws IOException {
		this.inputDoc = inputDoc;
		this.domain = inputDoc.getDomain();

		init(numComparedDocs);
	}

	private void init(int numComparedDocs) throws IOException {
		Directory dir = FSDirectory.open(new File("src/main/resources/novelty-index/" + domain));

		this.ir = DirectoryReader.open(dir);
		this.is = new IndexSearcher(this.ir);

		if (!Searcher.doesDocExist(is, inputDoc.getId())) {
			Indexer.addToIndex(dir, this.inputDoc, is);
			Indexer.removeFromIndex(dir, Searcher.getDocWithLowestId(is));

			this.ir.close();
			this.ir = DirectoryReader.open(dir);
			this.is = new IndexSearcher(this.ir);
		} else {
			System.out.println("Document already in index.");
		}

		this.preselectedPosts.putAll(Searcher.getSimilarPosts(ir, is, this.inputDoc, numComparedDocs));
	}

	private List<Double> computeTermAspectSimilarity() throws IOException {
		Map<String, Double> newPostTfidf = getTfidf(Searcher.getDocId(is, inputDoc.getId()));
		List<Map<String, Double>> preselectedPostsTfidf = new ArrayList<>();

		for (int docId : preselectedPosts.keySet()) {
			preselectedPostsTfidf.add(getTfidf(docId));
		}

		RealVector newPostVector = transformToVector(newPostTfidf);
		List<RealVector> preselectedPostsVectors = new ArrayList<>();

		for (Map<String, Double> preselectedPostTfidf : preselectedPostsTfidf) {
			preselectedPostsVectors.add(transformToVector(preselectedPostTfidf));
		}

		List<Double> cosineSims = new ArrayList<>();

		for (RealVector preselectedPostVector : preselectedPostsVectors) {
			double cosineSim = newPostVector.dotProduct(preselectedPostVector) / (newPostVector.getNorm() * preselectedPostVector.getNorm());
			cosineSims.add(cosineSim);
		}

		return cosineSims;
	}

	private Map<String, Double> getTfidf(int docId) throws IOException {
		Map<String, Integer> termFreqs = new LinkedHashMap<>();
		Map<String, Integer> docFreqs = new LinkedHashMap<>();
		Map<String, Double> tfidfWeights = new LinkedHashMap<>();

		Terms termVector = ir.getTermVector(docId, "body");
		TermsEnum termsEnum = termVector.iterator(null);
		BytesRef term = null;
		int numDocs = ir.numDocs();

		while ((term = termsEnum.next()) != null) {
			String termText = term.utf8ToString();
			Term termInstance = new Term("body", term);
			int df = ir.docFreq(termInstance);

			docFreqs.put(termText, df);

			DocsEnum docsEnum = termsEnum.docs(null, null);

			while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
				int tf = docsEnum.freq();
				termFreqs.put(termText, tf);
			}

			terms.add(termText);
		}

		for (String termText : docFreqs.keySet()) {
			double tf = 1 + Math.log(termFreqs.get(termText));
			double df = docFreqs.get(termText);
			double idf = Math.log(numDocs / df);
			double tfidf = tf * idf;

			tfidfWeights.put(termText, tfidf);
		}

		return tfidfWeights;
	}

	private RealVector transformToVector(Map<String, Double> tfidfWeights) {
		RealVector vector = new ArrayRealVector(terms.size());
		int i = 0;
		double tfidf = 0;

		for (String term : terms) {
			if (tfidfWeights.containsKey(term)) {
				tfidf = tfidfWeights.get(term);
			} else {
				tfidf = 0;
			}
			vector.setEntry(i++, tfidf);
		}

		return (RealVector) vector.mapDivide(vector.getL1Norm()); // Normalization since topic aspect switch to cosine
	}

	private List<Double> computeTopicAspectSimilarityHellinger() {
		List<Double> hellingerDists = new ArrayList<>();
		double[] newPostTopicWeights = inputDoc.getTopicWeights();

		for (Post preselectedPost : preselectedPosts.values()) {
			double hellingerDist = 0;
			double[] preselectedPostTopicWeights = preselectedPost.getTopicWeights();

			for (int i = 0; i < newPostTopicWeights.length; i++) {
				hellingerDist += Math.pow(Math.sqrt(newPostTopicWeights[i]) - Math.sqrt(preselectedPostTopicWeights[i]), 2);
			}

			hellingerDist = Math.sqrt(hellingerDist) / Math.sqrt(2);
			hellingerDists.add(1 - hellingerDist);
		}

		return hellingerDists;
	}

	@SuppressWarnings("unused")
	private List<Double> computeTopicAspectSimiliarityCosine() {
		RealVector newPostTopicVector = transformTopicWeightsToVector(inputDoc.getStringTopicWeights());
		RealVector preselectedPostTopicVector;
		List<Double> topicAspectCosineSims = new ArrayList<>();

		for (Post preselectedPost : preselectedPosts.values()) {
			preselectedPostTopicVector = transformTopicWeightsToVector(preselectedPost.getStringTopicWeights());
			double cosineSim = newPostTopicVector.dotProduct(preselectedPostTopicVector)
					/ (newPostTopicVector.getNorm() * preselectedPostTopicVector.getNorm());
			topicAspectCosineSims.add(cosineSim);
		}

		return topicAspectCosineSims;
	}

	private RealVector transformTopicWeightsToVector(String[] topicWeightsStrings) {
		int topicNum = topicWeightsStrings.length;
		RealVector vector = new ArrayRealVector(topicNum);

		for (int i = 0; i < topicNum; i++) {
			vector.setEntry(i, Double.valueOf(topicWeightsStrings[i]).doubleValue());
		}

		return (RealVector) vector.mapDivide(vector.getL1Norm());
	}

	private List<Double> computeCombinedSimilarity(double weight) throws IOException {
		List<Double> TermAspectSims = computeTermAspectSimilarity();
		List<Double> TopicAspectSims = computeTopicAspectSimilarityHellinger();
		// List<Double> TopicAspectSims = computeTopicAspectSimiliarityCosine();
		List<Double> combinedSims = new ArrayList<>();

		for (int i = 0; i < preselectedPosts.size(); i++) {
			double combinedSim = (TermAspectSims.get(i) * weight) + (TopicAspectSims.get(i) * (1 - weight));
			combinedSims.add(combinedSim);
		}

		return combinedSims;
	}

	public void computeMaxNovelty(double weight, double threshold) throws IOException {
		List<Double> combinedSims = computeCombinedSimilarity(weight);
		boolean novel = true;

		for (int i = 0; i < combinedSims.size(); i++) {
			if ((1 - combinedSims.get(i)) < threshold) {
				novel = false;
			}
		}

		Collections.sort(combinedSims, Collections.reverseOrder());
		if (!combinedSims.isEmpty()) {
			inputDoc.setNoveltyScore(1 - combinedSims.get(0));
			inputDoc.setNovel(novel);
		}
	}

	public void computeMeanNovelty(double weight, double threshold) throws IOException {
		List<Double> combinedSims = computeCombinedSimilarity(weight);
		double totSim = 0;
		boolean novel = true;

		for (int i = 0; i < combinedSims.size(); i++) {
			totSim += combinedSims.get(i);
		}

		double meanSim = totSim / combinedSims.size();

		if ((1 - meanSim) < threshold) {
			novel = false;
		}

		inputDoc.setNovel(novel);
		inputDoc.setNoveltyScore(1 - meanSim);
	}
}
