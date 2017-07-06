package xdomainnlp.namedentityrecognition;

import java.util.Collections;
import java.util.HashMap;
import org.openrdf.model.URI;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.graph.weight.GWS_impl;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.sml.sm.core.utils.SMConstants;
import slib.utils.ex.SLIB_Ex_Critic;
import slib.utils.ex.SLIB_Exception;

public class GraphTraverser {
	private URIFactory factory;
	private G graph;
	private SM_Engine engine;
	private ICconf icConf;

	public GraphTraverser(String domain, String ontologyBaseUri) throws SLIB_Exception {
		this.factory = URIFactoryMemory.getSingleton();
		URI baseUri = factory.getURI(ontologyBaseUri);
		graph = new GraphMemory(baseUri);
		String filePath = "src/main/resources/ontology/" + domain + "/" + AnnotationMerger.PROPERTIES_FILE_INFERRED_ONTOLOGY + ".ttl";
		GDataConf graphConf = new GDataConf(GFormat.TURTLE, filePath);
		GraphLoaderGeneric.populate(graphConf, graph);
		GAction addRoot = new GAction(GActionType.REROOTING);
		GraphActionExecutor.applyAction(addRoot, graph);
		engine = new SM_Engine(graph);
		icConf = new IC_Conf_Topo("Sanchez", SMConstants.FLAG_ICI_SANCHEZ_2011);
	}

	public String getCommonAncestor(String candidateASubj, String candidateBSubj) throws SLIB_Exception {
		URI candidateA = factory.getURI(candidateASubj);
		URI candidateB = factory.getURI(candidateBSubj);
		URI commonAncestor = engine.getMICA(icConf, candidateA, candidateB);

		return commonAncestor.stringValue();
	}

	public String getDirectAncestor(String candidateSubj) {
		String directAncestor = "";
		double maxPathToRoot = 0;
		HashMap<Double, URI> parentsByShortestPathToRoot = new HashMap<>();
		URI candidate = factory.getURI(candidateSubj);
		GWS_impl gws = new GWS_impl();

		try {
			for (URI parent : engine.getParents(candidate)) {
				parentsByShortestPathToRoot.put(engine.getShortestPath(parent, engine.getRoot(), gws), parent);
			}
		} catch (SLIB_Ex_Critic e) {
			e.printStackTrace();
		}

		maxPathToRoot = Collections.max(parentsByShortestPathToRoot.keySet());
		directAncestor = parentsByShortestPathToRoot.get(maxPathToRoot).stringValue();

		return directAncestor;
	}
}
