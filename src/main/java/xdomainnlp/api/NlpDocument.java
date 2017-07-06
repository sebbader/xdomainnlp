package xdomainnlp.api;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = Post.class)
public class NlpDocument {

	private String domain;
	private String mongoDocId;
	private String id;
	private String title;
	private String body;

	public NlpDocument() {

	}

	@JsonCreator
	public NlpDocument(@JsonProperty("domain") String domain, @JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("body") String body) {
		this.domain = domain;
		this.id = id;
		this.title = title;
		this.body = body;
	}

	@JsonProperty("domain")
	public String getDomain() {
		return domain;
	}

	@JsonProperty("domain")
	public void setDomain(String domain) {
		this.domain = domain;
	}

	@JsonProperty("mongoDocId")
	public String getMongoDocId() {
		return mongoDocId;
	}

	@JsonProperty("mongoDocId")
	public void setMongoDocId(String mongoDocId) {
		this.mongoDocId = mongoDocId;
	}

	@JsonProperty("id")
	public String getId() {
		return id;
	}

	@JsonProperty("id")
	public void setId(String id) {
		this.id = id;
	}

	@JsonProperty("title")
	public String getTitle() {
		return title;
	}

	@JsonProperty("title")
	public void setTitle(String title) {
		this.title = title;
	}

	@JsonProperty("body")
	public String getBody() {
		return body;
	}

	@JsonProperty("body")
	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return new StringWriter().append("id: ").append(this.id).append(" title: ").append(this.title).append(" body: ").append(this.body).toString();
	}

}
