package xdomainnlp.resources;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.lucene.queryparser.classic.ParseException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import io.dropwizard.validation.OneOf;
import xdomainnlp.api.NoveltyParameter;
import xdomainnlp.api.Post;
import xdomainnlp.core.XdomainNlpCore;
import xdomainnlp.db.TriplestoreManagerImpl;
import xdomainnlp.namedentityrecognition.TripleIndexer;
import xdomainnlp.noveltydetection.Indexer;

@Path("/nlp")
public class XdomainNlpResource {
	static final Logger LOGGER = LoggerFactory.getLogger(XdomainNlpResource.class);
	private XdomainNlpCore xDomainNlpCore;
	private MongoDatabase mongoDb;
	private TriplestoreManagerImpl tripleStoreManager;

	public XdomainNlpResource(XdomainNlpCore xDomainNlpCore, MongoDatabase mongoDb) {
		this.xDomainNlpCore = xDomainNlpCore;
		this.mongoDb = mongoDb;
		this.tripleStoreManager = new TriplestoreManagerImpl(System.getProperty("user.home") + "/rdf-repository/");
	}

	/**
	 * Create annotation resource.
	 * 
	 * @param reqAnnotator
	 *            Annotator that is requested via REST API.
	 * @param inputDoc
	 *            Document that has to be annotated.
	 * @return Confirm message with location of the created annotation resource.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Path("/{domain}/{reqAnnotator}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createAnnotation(@Context UriInfo uriInfo, @PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner" }) String reqAnnotator,
			@QueryParam("computeNovelty") @DefaultValue("false") boolean noveltyComputed,
			@QueryParam("weight") @DefaultValue("0.75") @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal weight,
			@QueryParam("threshold") @DefaultValue("0.85") @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal threshold, Post inputDoc)
			throws InterruptedException, IOException, ParseException {
		File nerOntologyIndexDir = new File("src/main/resources/ontology-index/" + domain);
		File noveltyIndexDir = new File("src/main/resources/novelty-index/" + domain);
		String serviceBaseUri = uriInfo.getBaseUri().toString();
		NoveltyParameter noveltyParam = new NoveltyParameter(noveltyComputed, weight, threshold);

		if (reqAnnotator.equals("ner")) {
			if (!(nerOntologyIndexDir.isDirectory() || nerOntologyIndexDir.list().length > 0)) {
				LOGGER.info("No ontology found for NER in domain " + "'" + domain + "'." + "Running statistical NER only.");
			}
		}

		if (noveltyParam.isNoveltyComputed()) {
			if (!(noveltyIndexDir.isDirectory() && noveltyIndexDir.list().length > 0)) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No index found for novelty detection in domain " + "'" + domain + "'.")
						.build();
			}
		}

		Document mongoDoc = new Document().append("domain", domain).append("id", inputDoc.getId()).append("title", inputDoc.getTitle()).append("body",
				inputDoc.getBody());

		if (domain.equals("stackoverflow") && inputDoc.getTags().length > 0) {
			mongoDoc.append("tags", Arrays.asList(inputDoc.getTags()));
		}

		mongoDb.getCollection(reqAnnotator).insertOne(mongoDoc);
		String mongoDocId = mongoDoc.getObjectId("_id").toString();
		inputDoc.setMongoDocId(mongoDocId);
		URI annotationLocation = uriInfo.getAbsolutePath().resolve(reqAnnotator + "/" + mongoDocId + "/annotation");
		inputDoc.setDomain(domain);

		xDomainNlpCore.runPipeline(inputDoc, noveltyParam, reqAnnotator, serviceBaseUri, tripleStoreManager);

		if (!mongoDoc.isEmpty()) {
			return Response.created(annotationLocation).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Annotation failed.").build();
		}
	}

	@Path("/{domain}/ontology-index")
	@POST
	public Response createOntologyIndex(@PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain) throws IOException {
		TripleIndexer tripleIndexer = new TripleIndexer(domain);
		tripleIndexer.indexOntology();

		return Response.ok().entity("Index created.").build();
	}

	@Path("/{domain}/novelty-index")
	@POST
	public Response createNoveltyIndex(@PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain) throws IOException {
		Indexer indexer = new Indexer(domain);
		indexer.createIndex();

		return Response.ok().entity("Index created.").build();
	}

	/**
	 * Get raw input text of the corresponding annotation.
	 * 
	 * @param uriInfo
	 *            Web Service URI.
	 * @param reqAnnotator
	 *            Requested annotator.
	 * @param id
	 *            Annotation ID
	 * @return Raw input text.
	 */
	@Path("/{domain}/{reqAnnotator}/{id}/document")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readAnnotationText(@Context UriInfo uriInfo, @PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id) {
		Document mongoDoc = mongoDb.getCollection(reqAnnotator).find(Filters.eq("_id", new ObjectId(id))).first();
		String result = mongoDoc.toJson();

		if (result != null) {
			return Response.ok(result).build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	/**
	 * Get entire annotation.
	 * 
	 * @param uriInfo
	 *            Web Service URI.
	 * @param reqAnnotator
	 *            Requested annotator.
	 * @param id
	 *            Annotation ID
	 * @param format
	 *            Serialization format.
	 * @return Annotation.
	 */
	@Path("/{domain}/{reqAnnotator}/{id}/annotation")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, "application/ld+json", "text/turtle" })
	public Response readAnnotationDoc(@Context UriInfo uriInfo, @PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id,
			@QueryParam("format") @OneOf(value = { "json-ld", "turtle" }) @DefaultValue("json-ld") String format) {
		String baseUri = uriInfo.getBaseUri().toString();
		String result = tripleStoreManager.serializeRdfModel(tripleStoreManager.readRdfDoc(baseUri, domain, reqAnnotator, id), format);

		if (result != null) {
			return Response.ok(result).build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	/**
	 * List all annotated sentences in given document.
	 * 
	 * @param uriInfo
	 *            Web Service URI.
	 * @param reqAnnotator
	 *            Requested annotator.
	 * @param id
	 *            Annotation ID
	 * @param format
	 *            Serialization format.
	 * @return Annotation.
	 */
	@Path("/{domain}/{reqAnnotator}/{id}/annotation/sentence")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, "application/ld+json", "text/turtle" })
	public Response readAnnotationSentences(@Context UriInfo uriInfo,
			@PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id,
			@PathParam("sentenceIndex") int sentenceIndex,
			@QueryParam("format") @OneOf(value = { "json-ld", "turtle" }) @DefaultValue("json-ld") String format) {

		String baseUri = uriInfo.getBaseUri().toString();
		String result = tripleStoreManager.serializeRdfModel(tripleStoreManager.readRdfSentences(baseUri, domain, reqAnnotator, id), format);

		if (result != null) {
			return Response.ok(result).build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	/**
	 * Get annotated sentence.
	 * 
	 * @param uriInfo
	 *            Web Service URI.
	 * @param reqAnnotator
	 *            Requested annotator.
	 * @param id
	 *            Annotation ID
	 * @param format
	 *            Serialization format.
	 * @return Annotation.
	 */
	@Path("/{domain}/{reqAnnotator}/{id}/annotation/sentence/{sentenceIndex}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, "application/ld+json", "text/turtle" })
	public Response readAnnotationSentence(@Context UriInfo uriInfo,
			@PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id,
			@PathParam("sentenceIndex") int sentenceIndex,
			@QueryParam("format") @OneOf(value = { "json-ld", "turtle" }) @DefaultValue("json-ld") String format) {

		String baseUri = uriInfo.getBaseUri().toString();
		String result = tripleStoreManager.serializeRdfModel(tripleStoreManager.readRdfSentence(baseUri, domain, reqAnnotator, id, sentenceIndex),
				format);

		if (result != null) {
			return Response.ok(result).build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	@Path("/{domain}/{reqAnnotator}/{id}/annotation/pos-tag")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, "application/ld+json", "text/turtle" })
	public Response readAnnotationPOS(@Context UriInfo uriInfo, @PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id,
			@PathParam("sentenceIndex") int sentenceIndex,
			@QueryParam("format") @OneOf(value = { "json-ld", "turtle" }) @DefaultValue("json-ld") String format) {

		String baseUri = uriInfo.getBaseUri().toString();
		String result = tripleStoreManager.serializeRdfModel(tripleStoreManager.readRdfPosTags(baseUri, domain, reqAnnotator, id), format);

		if (result != null && reqAnnotator.equals("pos")) {
			return Response.ok(result).build();
		} else if (reqAnnotator.equals("ner")) {
			return Response.status(Status.NOT_FOUND).entity("There are no POS tags in NER annotations.").build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	@Path("/{domain}/{reqAnnotator}/{id}/annotation/entity")
	@GET
	@Produces({ MediaType.APPLICATION_JSON, "application/ld+json", "text/turtle" })
	public Response readAnnotationEntity(@Context UriInfo uriInfo, @PathParam("domain") @OneOf(value = { "general", "stackoverflow" }) String domain,
			@PathParam("reqAnnotator") @OneOf(value = { "pos", "ner", "depparse" }) String reqAnnotator, @PathParam("id") String id,
			@PathParam("sentenceIndex") int sentenceIndex,
			@QueryParam("format") @OneOf(value = { "json-ld", "turtle" }) @DefaultValue("json-ld") String format) {

		String baseUri = uriInfo.getBaseUri().toString();
		String result = tripleStoreManager.serializeRdfModel(tripleStoreManager.readRdfEntities(baseUri, domain, reqAnnotator, id), format);

		if (result != null && reqAnnotator.equals("ner")) {
			return Response.ok(result).build();
		} else if (reqAnnotator.equals("pos")) {
			return Response.status(Status.NOT_FOUND).entity("There are no named entities in POS annotations.").build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}

	/**
	 * Deletes certain annotation resource from database.
	 * 
	 * @param requestedAnnotator
	 *            Annotator that is requested via REST API.
	 * @param id
	 *            ID of the annotation resource that should be deleted.
	 */
	@Path("/{domain}/{requestedAnnotator}/{id}")
	@DELETE
	public Response deleteAnnotation(
			@PathParam("requestedAnnotator") @OneOf(value = { "pos", "ner", "depparse", "dcoref" }) String requestedAnnotator,
			@PathParam("id") String id) {

		Document mongoDocument = mongoDb.getCollection(requestedAnnotator).find(Filters.eq("_id", new ObjectId(id))).first();

		if (mongoDocument != null) {
			this.mongoDb.getCollection(requestedAnnotator).deleteOne(Filters.eq("_id", new ObjectId(id)));
			return Response.ok("Resource with ID " + id + " deleted.").build();
		} else {
			return Response.status(Status.NOT_FOUND).entity("Resource with ID " + id + " not found.").build();
		}
	}
}
