package xdomainnlp;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import xdomainnlp.core.XdomainNlpCore;
import xdomainnlp.db.MongoClientManager;
import xdomainnlp.health.MongoHealthCheck;
import xdomainnlp.resources.XdomainNlpResource;

public class XdomainNlpApplication extends Application<XdomainNlpConfiguration> {

	public static void main(final String[] args) throws Exception {
		new XdomainNlpApplication().run(args);
	}

	@Override
	public String getName() {
		return "Cross-domain NLP";
	}

	@Override
	public void initialize(final Bootstrap<XdomainNlpConfiguration> bootstrap) {
		// Optional initialization.
	}

	@Override
	public void run(final XdomainNlpConfiguration configuration, final Environment environment) {
		/*
		 * Create new Mongo client and database.
		 */
		final MongoClient mongoClient = new MongoClient(configuration.mongohost, configuration.mongoport);
		final MongoDatabase mongoDatabase = mongoClient.getDatabase(configuration.mongodb);
		final MongoClientManager mongoClientManager = new MongoClientManager(mongoClient);

		/*
		 * Create new NLP core.
		 */
		final XdomainNlpCore xDomainNlpCore = new XdomainNlpCore();

		environment.lifecycle().manage(mongoClientManager);
		environment.healthChecks().register("mongoDB", new MongoHealthCheck(mongoClient));
		environment.jersey().register(new XdomainNlpResource(xDomainNlpCore, mongoDatabase));
	}
}
