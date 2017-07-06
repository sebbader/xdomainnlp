package xdomainnlp.namedentityrecognition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import xdomainnlp.api.NgramAnalysisResult;

public class TripleSearcher {
	public static final String FIELD_SUBJECT = "subject";
	public static final String FIELD_PREDICATE = "predicate";
	public static final String FIELD_OBJECT_URI = "object_uri";
	public static final String FIELD_OBJECT_LITERAL = "object_literal";
	public static final String FIELD_OBJECT_LITERAL_2GRAM = "2gram";
	public static final String FIELD_OBJECT_LITERAL_3GRAM = "3gram";
	public static final String FIELD_OBJECT_LITERAL_4GRAM = "4gram";
	public static final String FIELD_OBJECT_LITERAL_3GRAM_BEGIN_EDGE = "3gramBeginEdge";
	public static final String FIELD_OBJECT_LITERAL_4GRAM_BEGIN_EDGE = "4gramBeginEdge";
	public static final String FIELD_OBJECT_LITERAL_3GRAM_END_EDGE = "3gramEndEdge";
	public static final String FIELD_OBJECT_LITERAL_4GRAM_END_EDGE = "4gramEndEdge";

	private String idxPath;

	private MMapDirectory mmapDir;
	private IndexSearcher idxSearcher;
	private DirectoryReader idxReader;
	private UrlValidator urlValidator;
	StringUtils isInt = new StringUtils();
	private ValueFactory valueFactory;
	private int queryCount;

	public TripleSearcher(String domain) throws IOException {
		this.idxPath = "src/main/resources/ontology-index/" + domain;
		mmapDir = new MMapDirectory(new File(idxPath));
		idxReader = DirectoryReader.open(mmapDir);
		idxSearcher = new IndexSearcher(idxReader);
		this.urlValidator = new UrlValidator();
		this.valueFactory = SimpleValueFactory.getInstance();
		this.queryCount = 0;
	}

	public ArrayList<Statement> search(String subject, String predicate, String object, int numDocsRetrieved, double matchFactor) {
		BooleanQuery bq = new BooleanQuery();
		ArrayList<Statement> result = new ArrayList<>();

		try {
			if (subject != null) {
				Query tq = new TermQuery(new Term(FIELD_SUBJECT, subject));
				bq.add(tq, BooleanClause.Occur.MUST);
			}

			if (predicate != null) {
				Query tq = new TermQuery(new Term(FIELD_PREDICATE, predicate));
				bq.add(tq, BooleanClause.Occur.MUST);
			}

			if (object != null) {
				Query q = null;

				if (urlValidator.isValid(object)) {
					q = new TermQuery(new Term(FIELD_OBJECT_URI, object));
					bq.add(q, BooleanClause.Occur.MUST);
				} else if (StringUtils.isNumeric(object)) {
					int tempInt = Integer.parseInt(object);
					BytesRef bytesRef = new BytesRef(NumericUtils.BUF_SIZE_INT);
					NumericUtils.intToPrefixCoded(tempInt, 0, bytesRef);
					q = new TermQuery(new Term(FIELD_OBJECT_LITERAL, bytesRef.utf8ToString()));
					bq.add(q, BooleanClause.Occur.MUST);
				} else if (predicate.equals(RDFS.LABEL.stringValue())) {
					queryCount = 0;
					NgramAnalysisResult ngramAnalysisResult = LookupAnalyzerFactory.analyzeNgrams(object);

					addNGramQuery(bq, FIELD_OBJECT_LITERAL_2GRAM, ngramAnalysisResult.gram2s, 1.0);
					addNGramQuery(bq, FIELD_OBJECT_LITERAL_3GRAM, ngramAnalysisResult.gram3s, 1.0);
					addNGramQuery(bq, FIELD_OBJECT_LITERAL_4GRAM, ngramAnalysisResult.gram4s, 1.0);
					addEdgeQuery(bq, FIELD_OBJECT_LITERAL_3GRAM_BEGIN_EDGE, ngramAnalysisResult.gramBeginEdge3, 1.0);
					addEdgeQuery(bq, FIELD_OBJECT_LITERAL_4GRAM_BEGIN_EDGE, ngramAnalysisResult.gramBeginEdge4, 1.0);
					addEdgeQuery(bq, FIELD_OBJECT_LITERAL_3GRAM_END_EDGE, ngramAnalysisResult.gramEndEdge3, 1.0);
					addEdgeQuery(bq, FIELD_OBJECT_LITERAL_4GRAM_END_EDGE, ngramAnalysisResult.gramEndEdge4, 1.0);

					bq.setMinimumNumberShouldMatch((int) Math.ceil(queryCount * matchFactor));
				} else if (!object.contains(" ")) {
					Analyzer analyzer = LookupAnalyzerFactory.createLiteralAnalyzer();
					q = new QueryParser(Version.LUCENE_44, FIELD_OBJECT_LITERAL, analyzer).parse(object);
					bq.add(q, BooleanClause.Occur.MUST);
				} else {
					Analyzer analyzer = LookupAnalyzerFactory.createLiteralAnalyzer();
					QueryParser qParser = new QueryParser(Version.LUCENE_44, FIELD_OBJECT_LITERAL, analyzer);
					qParser.setDefaultOperator(QueryParser.Operator.AND);
					q = qParser.parse(QueryParserBase.escape(object));
					bq.add(q, BooleanClause.Occur.MUST);
				}
			}
			result = getFromIndex(numDocsRetrieved, bq);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private ArrayList<Statement> getFromIndex(int numDocsRetrieved, BooleanQuery bq) throws IOException {
		ArrayList<Statement> result = new ArrayList<>();
		String subjectString;
		String predicateString;
		String objectString;
		Resource subject = null;
		IRI predicate = null;
		Value object = null;

		TopScoreDocCollector collector = TopScoreDocCollector.create(numDocsRetrieved, true);
		idxSearcher.search(bq, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = idxSearcher.doc(hits[i].doc);
			subjectString = hitDoc.get(FIELD_SUBJECT);
			predicateString = hitDoc.get(FIELD_PREDICATE);
			objectString = hitDoc.get(FIELD_OBJECT_URI);

			if (objectString == null) {
				objectString = hitDoc.get(FIELD_OBJECT_LITERAL);
			}

			if (urlValidator.isValid(subjectString)) {
				subject = valueFactory.createIRI(subjectString);
			} else {
				subject = valueFactory.createBNode(subjectString);
			}

			predicate = valueFactory.createIRI(predicateString);

			if (urlValidator.isValid(objectString)) {
				object = valueFactory.createIRI(objectString);
				result.add(valueFactory.createStatement(subject, predicate, object));
			} else {
				object = valueFactory.createLiteral(objectString);
				result.add(valueFactory.createStatement(subject, predicate, object));
			}
		}

		return result;
	}

	private void addNGramQuery(BooleanQuery query, String fieldName, List<String> grams, double boost) {
		for (String gram : grams) {
			TermQuery nGramQuery = new TermQuery(new Term(fieldName, gram));
			nGramQuery.setBoost((float) boost);
			query.add(nGramQuery, Occur.SHOULD);

			++queryCount;
		}
	}

	private void addEdgeQuery(BooleanQuery query, String fieldName, String fieldValue, double boost) {
		TermQuery edgeQuery = new TermQuery(new Term(fieldName, fieldValue));
		edgeQuery.setBoost((float) boost);
		query.add(edgeQuery, Occur.SHOULD);

		++queryCount;
	}

	public void close() throws IOException {
		idxReader.close();
		mmapDir.close();
	}
}
