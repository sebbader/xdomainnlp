package xdomainnlp.api;

import java.util.ArrayList;
import java.util.List;

public class NgramAnalysisResult {
	public String input;
	public List<String> gram2s = new ArrayList<String>();
	public List<String> gram3s = new ArrayList<String>();
	public List<String> gram4s = new ArrayList<String>();
	public String gramBeginEdge3 = "";
	public String gramBeginEdge4 = "";
	public String gramEndEdge3 = "";
	public String gramEndEdge4 = "";
}
