package xdomainnlp.noveltydetection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import xdomainnlp.api.Post;
import xdomainnlp.noveltydetection.NoveltyDetectorAnalyzerFactory;

public class Indexer {
	public static final String FIELD_ID = "id";
	public static final String FIELD_TITLE = "title";
	public static final String FIELD_BODY = "body";
	public static final String FIELD_TAG = "tag";
	public static final String FIELD_TOPIC_WEIGHT = "topicWeight";

	public static final FieldType TYPE_STORED = new FieldType();
	public static final HashMap<String, Analyzer> PER_FIELD_ANALYZERS = new HashMap<String, Analyzer>();

	private String domain;

	static {
		TYPE_STORED.setIndexed(true);
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(false);
		TYPE_STORED.setStoreTermVectors(true);
		TYPE_STORED.setStoreTermVectorPositions(true);

		PER_FIELD_ANALYZERS.put(FIELD_ID, new KeywordAnalyzer());
		PER_FIELD_ANALYZERS.put(FIELD_TITLE, NoveltyDetectorAnalyzerFactory.createTitleIndexAnalyzer());
		PER_FIELD_ANALYZERS.put(FIELD_BODY, NoveltyDetectorAnalyzerFactory.createBodyIndexAnalyzer());
		PER_FIELD_ANALYZERS.put(FIELD_TAG, new WhitespaceAnalyzer(Version.LUCENE_44));
		PER_FIELD_ANALYZERS.put(FIELD_TOPIC_WEIGHT, new WhitespaceAnalyzer(Version.LUCENE_44));
	}

	public Indexer(String domain) {
		this.domain = domain;
	}

	public void createIndex() throws IOException {
		File indexDir = new File("src/main/resources/novelty-index/" + domain);
		File dataDir = new File("src/main/resources/novelty-data/" + domain);

		if (!indexDir.isDirectory()) {
			indexDir.mkdirs();
		}

		Directory dir = FSDirectory.open(indexDir);
		PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(Version.LUCENE_44), PER_FIELD_ANALYZERS);

		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzerWrapper);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(dir, iwc);

		for (File file : dataDir.listFiles()) {
			if (file.getName().endsWith("txt")) {
				createDocs(iw, file);
			}
		}

		iw.commit();
		iw.close();
	}

	private void createDocs(IndexWriter iw, File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = "";

		while ((line = br.readLine()) != null) {
			List<String> lineParts = new ArrayList<String>(Arrays.asList(line.split("\t")));
			List<String> tags = new ArrayList<String>(Arrays.asList(lineParts.get(3).split(" ")));
			List<String> topicWeights = new ArrayList<String>(Arrays.asList(lineParts.get(4).split(" ")));

			Document doc = new Document();

			doc.add(new StringField(FIELD_ID, lineParts.get(0), Field.Store.YES));
			doc.add(new Field(FIELD_TITLE, lineParts.get(1), TYPE_STORED));
			doc.add(new Field(FIELD_BODY, lineParts.get(2), TYPE_STORED));

			for (String tag : tags) {
				doc.add(new TextField(FIELD_TAG, tag, Field.Store.YES));
			}

			for (String topicWeight : topicWeights) {
				doc.add(new StringField(FIELD_TOPIC_WEIGHT, topicWeight, Field.Store.YES));
			}

			iw.addDocument(doc);
		}

		br.close();
	}

	public static void addToIndex(Directory dir, Post post, IndexSearcher is) throws IOException {
		PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(Version.LUCENE_44), Indexer.PER_FIELD_ANALYZERS);

		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzerWrapper);
		iwc.setOpenMode(OpenMode.APPEND);
		iwc.setMaxBufferedDocs(2);
		IndexWriter iw = new IndexWriter(dir, iwc);

		Document doc = new Document();

		doc.add(new StringField(FIELD_ID, post.getId(), Field.Store.YES));
		doc.add(new Field(FIELD_TITLE, post.getTitle(), TYPE_STORED));
		doc.add(new Field(FIELD_BODY, post.getBody(), TYPE_STORED));

		for (String tag : post.getTags()) {
			doc.add(new TextField(FIELD_TAG, tag, Field.Store.YES));
		}

		if (post.getStringTopicWeights() != null) {
			for (String weight : post.getStringTopicWeights()) {
				doc.add(new StringField(FIELD_TOPIC_WEIGHT, weight, Field.Store.YES));
			}
		}

		if (Searcher.doesDocExist(is, post.getId())) {
			iw.updateDocument(new Term(FIELD_ID, post.getId()), doc);
		} else {
			iw.addDocument(doc);
		}

		iw.commit();
		iw.close();
	}

	public static void removeFromIndex(Directory dir, String lowestPostId) throws IOException {
		PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(Version.LUCENE_44), Indexer.PER_FIELD_ANALYZERS);

		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzerWrapper);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		IndexWriter iw = new IndexWriter(dir, iwc);

		iw.deleteDocuments(new Term(FIELD_ID, lowestPostId));

		iw.commit();
		iw.close();
	}
}
