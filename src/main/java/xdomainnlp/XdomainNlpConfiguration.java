package xdomainnlp;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class XdomainNlpConfiguration extends Configuration {

	@JsonProperty
	@NotEmpty
	public String mongohost = "localhost";

	@JsonProperty
	@Min(1)
	@Max(65535)
	public int mongoport = 27017;

	@JsonProperty
	@NotEmpty
	public String mongodb = "mongodb";
}
