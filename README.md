# Cross-domain NLP (xdomainnlp)

RESTful web service for all-purpose information extraction and novelty detection (ND) with the aid of exchangeable domain knowledge.

## Getting started

The xdomainnlp system allows you to
* thus far extract named entities (NER) with the use of ontologies and
* determine the degree of novelty of any incoming document compared to a set of already existing documents.

The following instructions show how to set up and run xdomainnlp.

## Prerequisites

Install a MongoDB server instance (preferably v3.4.3 and higher).

For the NER, an arbitrary ontology in the Turtle format (.ttl) is needed. xdomainnlp scans the rdfs:label property for potential entity matches.

For the novelty detection, the corpus should be one single text file (.txt) where each line represents one document. By default, each document consists of tab-separated ID, title and body, e. g.:

```
836436	Lorem ipsum	At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.
```

## Installation

### NER

1) In order to add a new domain and run the system for the first time, copy the ontology to resources/ontology/<domain-name> and execute a POST query to localhost:9080/nlp/{domain-name}/ontology-index. This creates an inverted index out of the ontology.
2) Create an ontology.properties file in the same directory containing the base URI of the ontology.
3) Optional - create the two files: named-entity-types.properties (mapping between the semantic types of an additional statistical NER model and ontology) and rules.n3 (LDFU inference rules) in the same directory.
4) Add the name of the domain as a path parameter in every method inside the resource class.
5) Optional - customize the noun phrase extraction rules in the positiveRules.txt and rejectionRules.txt files.

### ND

1) Analogous to the NER, copy the corpus to resources/novelty-data/<domain-name> and execute a POST query to localhost:9080/nlp/{domain-name}/ontology-index in order to add a new domain.

## Execution

1) Start MongoDB server.
2) Start xdomainnlp with the command:

```
java -Xmx3g -jar target/xdomainnlp-0.0.1.jar server dropwizard-config.yml
```

3) Use POST method with the following path parameters in order to input a document and extract its information:

```
http://localhost:9080/nlp/{domain}/{annotator}
```

Currently, only NER (and limited POS) is supported.

4) If you want to trigger the additional ND, use the following query parameters:

```
computeNovelty
weight
threshold
```

For example, if you want to run the ND in the default domain with a linear combination factor of 0.5 and a novelty threshold of 0.85, execute a POST query like so:

```
http://localhost:9080/nlp/general/ner?computeNovelty=true&weight=0.5&threshold=0.85
```

5) Use a GET query to retrieve the extraction result. Replace {id} with the document ID that was issued after the POST query. The following GET queries are currently implemented:

```
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/annotation
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/annotation/entity
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/annotation/pos-tag
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/annotation/sentence
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/annotation/sentence/{sentenceIndex}
http://localhost:9080/nlp/{domain}/{reqAnnotator}/{id}/document
```

6) Delete a document and its corresponding extractions with a DELETE query:

```
http://localhost:9080/nlp/{domain}/{requestedAnnotator}/{id}
```

## Built with

* Maven
* Dropwizard
* RDF4J
* Stanford CoreNLP
* Apache Lucene
* RBBNPE
* MALLET

* Based on stanfordNLPRESTAPI