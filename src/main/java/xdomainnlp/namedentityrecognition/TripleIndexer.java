package xdomainnlp.namedentityrecognition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

import xdomainnlp.api.NgramAnalysisResult;

public class TripleIndexer {
	private DirectoryReader dirReader;
	private IndexWriter idxWriter;
	private MMapDirectory mmapDir;

	private String ontoPath;
	private String ontoPropPath;
	private String idxPath;

	public TripleIndexer(String domain) {
		this.ontoPath = "src/main/resources/ontology/" + domain;
		this.ontoPropPath = "src/main/resources/ontology/" + domain + "/ontology.properties";
		this.idxPath = "src/main/resources/ontology-index/" + domain;
	}

	public void indexOntology() {
		try {
			Properties prop = new Properties();
			InputStream propIS = new FileInputStream(ontoPropPath);
			prop.load(propIS);
			String baseURI = prop.getProperty("ontologyBaseUri");

			List<File> files = new ArrayList<File>();

			for (File file : new File(ontoPath).listFiles()) {
				if (file.getName().endsWith("ttl")) {
					files.add(file);
				}
			}

			createIndex(files, idxPath, baseURI);
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createIndex(List<File> files, String idxPath, String baseURI) {
		Analyzer urlAnalyzer = LookupAnalyzerFactory.createUrlAnalyzer();
		Analyzer literalAnalyzer = LookupAnalyzerFactory.createLiteralAnalyzer();
		Analyzer ngramAnalyzer = LookupAnalyzerFactory.createNGramAnalyzer(2, 4);
		Analyzer ngramEdgeAnalyzer = LookupAnalyzerFactory.createNGramAnalyzer(3, 4);

		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();

		perFieldAnalyzers.put(TripleSearcher.FIELD_SUBJECT, urlAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_PREDICATE, urlAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_URI, urlAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL, literalAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_2GRAM, ngramAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM, ngramAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM, ngramAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM_BEGIN_EDGE, ngramEdgeAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM_BEGIN_EDGE, ngramEdgeAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM_END_EDGE, ngramEdgeAnalyzer);
		perFieldAnalyzers.put(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM_END_EDGE, ngramEdgeAnalyzer);

		PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(urlAnalyzer, perFieldAnalyzers);

		try {
			File idxDir = new File(idxPath);
			mmapDir = new MMapDirectory(idxDir);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzerWrapper);
			idxWriter = new IndexWriter(mmapDir, iwc);
			idxWriter.commit();

			for (File file : files) {
				indexTTLFiles(file, baseURI);
				idxWriter.commit();
			}

			idxWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void indexTTLFiles(File file, String baseURI) {
		try {
			InputStream is = new FileInputStream(file);
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			StatementHandler stmtHandler = new StatementHandler();
			rdfParser.setRDFHandler(stmtHandler);
			rdfParser.getParserConfig().setNonFatalErrors(new HashSet<RioSetting<?>>(Arrays.asList(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES)));
			rdfParser.parse(is, baseURI);
		} catch (RDFParseException | RDFHandlerException | IOException e) {
			e.printStackTrace();
		}
	}

	private void addDocumentToIndex(IndexWriter idxWriter, String subject, String predicate, String object, boolean isURI) throws IOException {
		Document doc = new Document();
		doc.add(new StringField(TripleSearcher.FIELD_SUBJECT, subject, Store.YES));
		doc.add(new StringField(TripleSearcher.FIELD_PREDICATE, predicate, Store.YES));

		if (isURI) {
			doc.add(new StringField(TripleSearcher.FIELD_OBJECT_URI, object, Store.YES));
		} else {
			doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL, object, Store.YES));

			if (predicate.equals(RDFS.LABEL.stringValue())) {
				NgramAnalysisResult result = LookupAnalyzerFactory.analyzeNgrams(object);

				for (String gram2 : result.gram2s) {
					doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_2GRAM, gram2, Field.Store.YES));
				}

				for (String gram3 : result.gram3s) {
					doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM, gram3, Field.Store.YES));
				}

				for (String gram4 : result.gram4s) {
					doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM, gram4, Field.Store.YES));
				}

				doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM_BEGIN_EDGE, result.gramBeginEdge3, Field.Store.YES));
				doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM_BEGIN_EDGE, result.gramBeginEdge4, Field.Store.YES));
				doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_3GRAM_END_EDGE, result.gramEndEdge3, Field.Store.YES));
				doc.add(new TextField(TripleSearcher.FIELD_OBJECT_LITERAL_4GRAM_END_EDGE, result.gramEndEdge4, Field.Store.YES));
			}
		}

		idxWriter.addDocument(doc);
	}

	private void close() throws IOException {
		if (dirReader != null) {
			dirReader.close();
		}

		if (mmapDir != null) {
			mmapDir.close();
		}
	}

	private class StatementHandler extends AbstractRDFHandler {
		@Override
		public void handleStatement(Statement stmt) {
			String subject = stmt.getSubject().stringValue();
			String predicate = stmt.getPredicate().stringValue();
			String object = stmt.getObject().stringValue();

			try {
				addDocumentToIndex(idxWriter, subject, predicate, object, stmt.getObject() instanceof IRI);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
