package xdomainnlp.api;

import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import xdomainnlp.interfaces.NifSentence;

/**
 * NIF entity corresponding to the CoreNLP annotations. Based on jplu's stanfordNLPRESTAPI.
 */
public class NifEntity {
	private String entitySurfaceForm;
	private String entityType;
	private NifSentence sentence;
	private NifContext nifContext;
	private int entityBeginOffset;
	private int entityEndOffset;
	private String disambiguatedURL;
	private ValueFactory vf;

	public NifEntity(String entitySurfaceForm, String entityType, NifSentence sentence, NifContext nifContext, int entityBeginOffset, int entityEndOffset,
			String disambiguatedURL) {
		this.entitySurfaceForm = entitySurfaceForm;
		this.entityType = entityType;
		this.sentence = sentence;
		this.nifContext = nifContext;
		this.entityBeginOffset = entityBeginOffset;
		this.entityEndOffset = entityEndOffset;
		this.disambiguatedURL = disambiguatedURL;
		this.vf = SimpleValueFactory.getInstance();
	}

	/**
	 * Transform entity into RDF model.
	 *
	 * @param serviceBaseUri
	 *            Host URI.
	 * @param domain
	 *            Current domain.
	 * @param mongoDocId
	 *            Assigned document ID.
	 *
	 * @return Entity RDF model.
	 */
	public final Model createRDFModel(final String serviceBaseUri, final String domain, final String mongoDocId) {
		Model rdfModel = new TreeModel();

		String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
		String itsrdf = "http://www.w3.org/2005/11/its/rdf#";
		String base = serviceBaseUri + "nlp/" + domain + "/" + mongoDocId + "/";
		String local = serviceBaseUri + "nlp/" + domain + "/" + "ontology/";

		rdfModel.setNamespace("itsrdf", itsrdf);

		IRI entitySubj = vf.createIRI(base, "entity#char=" + entityBeginOffset + "," + entityEndOffset);

		rdfModel.add(entitySubj, RDF.TYPE, vf.createIRI(nif, "String"));
		rdfModel.add(entitySubj, RDF.TYPE, vf.createIRI(nif, "RFC5147String"));
		rdfModel.add(entitySubj, RDF.TYPE, vf.createIRI(nif, "Phrase"));
		rdfModel.add(entitySubj, vf.createIRI(nif, "beginIndex"), vf.createLiteral(entityBeginOffset));
		rdfModel.add(entitySubj, vf.createIRI(nif, "endIndex"), vf.createLiteral(entityEndOffset));
		rdfModel.add(entitySubj, vf.createIRI(nif, "anchorOf"), vf.createLiteral(entitySurfaceForm));
		rdfModel.add(entitySubj, vf.createIRI(nif, "sentence"),
				vf.createIRI(base, "sentence#char=" + sentence.getSentenceBeginOffset() + "," + sentence.getSentenceEndOffset()));
		rdfModel.add(entitySubj, vf.createIRI(nif, "referenceContext"),
				vf.createIRI(base, "context#char=" + nifContext.getInputDocBeginOffset() + "," + nifContext.getInputDocEndOffset()));
		rdfModel.add(entitySubj, vf.createIRI(local, "type"), vf.createLiteral(entityType));

		UrlValidator urlValidator = new UrlValidator();

		if (disambiguatedURL != null && urlValidator.isValid(disambiguatedURL)) {
			rdfModel.add(entitySubj, vf.createIRI(itsrdf, "taIdentRef"), vf.createIRI(disambiguatedURL));
		}

		return rdfModel;
	}

	public final int getEntityBeginOffset() {
		return this.entityBeginOffset;
	}

	public final int getEntityEndOffset() {
		return this.entityEndOffset;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		final NifEntity entity = (NifEntity) obj;

		if (this.entityBeginOffset != entity.entityBeginOffset) {
			return false;
		}

		if (this.entityEndOffset != entity.entityEndOffset) {
			return false;
		}

		if (!this.entitySurfaceForm.equals(entity.entitySurfaceForm)) {
			return false;
		}

		if (!this.entityType.equals(entity.entityType)) {
			return false;
		}

		if (this.sentence.getSentenceIndex() != entity.sentence.getSentenceIndex()) {
			return false;
		}

		return this.nifContext.equals(entity.nifContext);
	}
}
