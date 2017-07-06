package xdomainnlp.api;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Post extends NlpDocument {
	private String[] tags;
	private String[] topicWeights;
	private boolean novel;
	private double noveltyScore;

	public Post() {

	}

	public Post(String domain, String id, String[] topicWeights) {
		super(domain, id, null, null);
		this.topicWeights = topicWeights;
	}

	@JsonCreator
	public Post(@JsonProperty("id") String id, @JsonProperty("title") String title, @JsonProperty("body") String body,
			@JsonProperty("tags") String[] tags) {
		super(null, id, title, body);
		this.tags = tags;
	}

	@JsonProperty("tags")
	public String[] getTags() {
		return tags;
	}

	@JsonProperty("tags")
	public void setTags(String[] tags) {
		this.tags = tags;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setStringTopicWeights(String[] topicWeights) {
		this.topicWeights = topicWeights;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public String[] getStringTopicWeights() {
		return topicWeights;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public double[] getTopicWeights() {
		double[] result = new double[this.topicWeights.length];

		for (int i = 0; i < this.topicWeights.length; i++) {
			result[i] = Double.valueOf(this.topicWeights[i]);
		}

		return result;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setTopicWeights(double[] topicWeights) {
		this.topicWeights = new String[topicWeights.length];

		for (int i = 0; i < topicWeights.length; i++) {
			this.topicWeights[i] = String.valueOf(topicWeights[i]);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public boolean isNovel() {
		return novel;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setNovel(boolean novel) {
		this.novel = novel;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public double getNoveltyScore() {
		return this.noveltyScore;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setNoveltyScore(double noveltyScore) {
		this.noveltyScore = noveltyScore;
	}

	@Override
	public String toString() {
		return new StringWriter().append("id: ").append(super.getId()).append(" title: ").append(super.getTitle()).append(" body: ")
				.append(super.getBody()).append(" tags: ").append(tags.toString()).toString();
	}

}
