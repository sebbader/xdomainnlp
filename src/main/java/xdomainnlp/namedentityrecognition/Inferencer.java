package xdomainnlp.namedentityrecognition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.turtle.TurtleParser;
import org.semanticweb.yars.turtle.TurtleParseException;

import edu.kit.aifb.datafu.Binding;
import edu.kit.aifb.datafu.ConstructQuery;
import edu.kit.aifb.datafu.Origin;
import edu.kit.aifb.datafu.Program;
import edu.kit.aifb.datafu.SelectQuery;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.notation3.ParseException;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;

public class Inferencer {
	private ValueFactory valueFactory;
	private String domain;
	private IRI baseUri;
	private String queryType;
	private String queryString;
	private UrlValidator urlValidator;

	public Inferencer(String domain, String baseUri, String queryType, String queryString) {
		this.valueFactory = SimpleValueFactory.getInstance();
		this.domain = domain;
		this.baseUri = valueFactory.createIRI(baseUri);
		this.queryType = queryType;
		this.queryString = queryString;
		this.urlValidator = new UrlValidator();
	}

	public void inferNewStmts() {
		Model model = new LinkedHashModel();

		/*
		 * Linked Data-Fu execution
		 */
		try {
			FileInputStream ontologyInputStream = new FileInputStream(new File("src/main/resources/ontology/" + domain + "/ontology.ttl"));
			FileInputStream rulesInputStream = new FileInputStream(new File("src/main/resources/ontology/" + domain + "/rules.n3"));

			/*
			 * Generate a Program Object
			 */
			Origin origin = new InternalOrigin("programOrigin");
			ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(origin);
			Notation3Parser notation3Parser = new Notation3Parser(rulesInputStream);
			notation3Parser.parse(programConsumer, origin);
			Program program = programConsumer.getProgram(origin);

			/*
			 * Generate a Graph Object
			 */
			TurtleParser turtleParser = new TurtleParser();
			turtleParser.parse(ontologyInputStream, Charset.defaultCharset(), new java.net.URI(baseUri.stringValue()));

			while (turtleParser.hasNext()) {
				Node[] node = turtleParser.next();
				Nodes nodes = new Nodes(node);
				program.addTriple(nodes);
			}

			/*
			 * Register a Query
			 */
			QueryConsumerImpl queryConsumer = new QueryConsumerImpl(new InternalOrigin("queryConsumer"));
			SparqlParser sparqlParser = new SparqlParser(new StringReader(queryString));
			sparqlParser.parse(queryConsumer, new InternalOrigin("sparqlDummy"));
			BindingConsumerCollection bindingConsumerCollection = new BindingConsumerCollection();
			BindingConsumerSink bindingConsumerSink = new BindingConsumerSink(bindingConsumerCollection);

			if (queryType.equals("select")) {
				SelectQuery selectQuery = queryConsumer.getSelectQueries().iterator().next();
				program.registerSelectQuery(selectQuery, bindingConsumerSink);
			} else if (queryType.equals("construct")) {
				ConstructQuery constructQuery = queryConsumer.getConstructQueries().iterator().next();
				program.registerConstructQuery(constructQuery, bindingConsumerSink);
			}

			/*
			 * Create an EvaluateProgram Object
			 */
			EvaluateProgramConfig evaluateProgramConfig = new EvaluateProgramConfig();
			EvaluateProgramGenerator evaluateProgramGenerator = new EvaluateProgramGenerator(program, evaluateProgramConfig);
			EvaluateProgram evaluateProgram = evaluateProgramGenerator.getEvaluateProgram();

			/*
			 * Evaluate the Program
			 */
			evaluateProgram.start();
			evaluateProgram.awaitIdleAndFinish();
			evaluateProgram.shutdown();

			for (Binding binding : bindingConsumerCollection.getCollection()) {
				Nodes nodes = binding.getNodes();
				Node[] node = nodes.getNodeArray();
				addStmtsToModel(node, model);
			}
		} catch (FileNotFoundException | ParseException | TurtleParseException | org.semanticweb.yars.turtle.ParseException | URISyntaxException
				| edu.kit.aifb.datafu.parser.sparql.ParseException | InterruptedException e) {
			e.printStackTrace();
		}

		try {
			writeInferredOntology(model);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addStmtsToModel(Node[] node, Model model) {
		Resource subject = null;
		IRI predicate = null;
		Value object = null;

		if (node[0] != null) {
			String subjectString = node[0].getLabel();

			if (urlValidator.isValid(subjectString)) {
				subject = valueFactory.createIRI(subjectString);
			} else {
				subject = valueFactory.createBNode(subjectString);
			}
		}

		if (node[1] != null) {
			String predicateString = node[1].getLabel();
			predicate = valueFactory.createIRI(predicateString);
		}

		if (node[2] != null) {
			String objectString = node[2].getLabel();

			if (urlValidator.isValid(objectString)) {
				object = valueFactory.createIRI(objectString);
				model.add(valueFactory.createStatement(subject, predicate, object));
			} else {
				object = valueFactory.createLiteral(objectString);
				model.add(valueFactory.createStatement(subject, predicate, object));
			}
		}
	}

	private void writeInferredOntology(Model model) throws IOException {
		FileOutputStream out = new FileOutputStream(
				new File("src/main/resources/ontology/" + domain + "/" + AnnotationMerger.PROPERTIES_FILE_INFERRED_ONTOLOGY + ".ttl"));
		try {
			Rio.write(model, out, RDFFormat.TURTLE);
		} finally {
			out.close();
		}
	}

	public ArrayList<Statement> readInferredOntology(String domain) throws IOException {
		ArrayList<Statement> result = new ArrayList<>();
		Model model = new LinkedHashModel();
		FileInputStream ontologyInputStream = new FileInputStream(new File("src/main/resources/ontology/" + domain + "/ontology.ttl"));
		TurtleParser turtleParser = new TurtleParser();

		try {
			turtleParser.parse(ontologyInputStream, Charset.defaultCharset(), new java.net.URI(baseUri.stringValue()));
		} catch (TurtleParseException | org.semanticweb.yars.turtle.ParseException | URISyntaxException e) {
			e.printStackTrace();
		}

		while (turtleParser.hasNext()) {
			Node[] node = turtleParser.next();
			addStmtsToModel(node, model);
		}

		for (Statement stmt : model) {
			result.add(stmt);
		}

		return result;
	}
}
