package xdomainnlp.health;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;

public class MongoHealthCheck extends HealthCheck {
	private MongoClient mongoClient;

	public MongoHealthCheck(MongoClient mongoClient) {
		super();
		this.mongoClient = mongoClient;
	}

	@Override
	protected Result check() throws Exception {
		try {
			mongoClient.listDatabaseNames();
		} catch (MongoClientException e) {
			return Result.unhealthy(e.getMessage());
		}
		return Result.healthy();
	}
}
