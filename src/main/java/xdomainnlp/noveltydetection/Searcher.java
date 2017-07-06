package xdomainnlp.noveltydetection;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;

import xdomainnlp.api.Post;

public class Searcher {
	public static LinkedHashMap<Integer, Post> getSimilarPosts(IndexReader ir, IndexSearcher is, Post post, int numComparedDocs) throws IOException {
		MoreLikeThis mlt = new MoreLikeThis(ir);

		mlt.setFieldNames(new String[] { Indexer.FIELD_TITLE, Indexer.FIELD_BODY });
		mlt.setMinTermFreq(1);
		mlt.setMinDocFreq(1);
		mlt.setMinWordLen(1);

		Query mltQuery = mlt.like(getDocId(is, post.getId()));
		TopDocs topDocs = null;

		if (post.getTags().length > 0) {
			FilteredQuery fq = createFilteredQuery(post.getTags(), mltQuery);
			topDocs = is.search(fq, numComparedDocs);
		} else {
			topDocs = is.search(mltQuery, numComparedDocs);
		}

		LinkedHashMap<Integer, Post> foundPosts = new LinkedHashMap<>();

		for (int i = 1; i < topDocs.scoreDocs.length; i++) {
			Document document = is.doc(topDocs.scoreDocs[i].doc);
			int docId = topDocs.scoreDocs[i].doc;
			String postId = document.get(Indexer.FIELD_ID);
			String[] topicWeights = document.getValues(Indexer.FIELD_TOPIC_WEIGHT);

			Post foundPost = new Post(null, postId, topicWeights);

			foundPosts.put(docId, foundPost);
		}

		return foundPosts;
	}

	private static BooleanQuery createTagQuery(String[] tags) {
		BooleanQuery bq = new BooleanQuery();

		bq.add(new TermQuery(new Term(Indexer.FIELD_TAG, tags[0])), Occur.MUST);

		if (tags.length > 1) {
			bq.add(new TermQuery(new Term(Indexer.FIELD_TAG, tags[1])), Occur.MUST);
		}

		return bq;
	}

	private static FilteredQuery createFilteredQuery(String[] tags, Query mltQuery) {
		QueryWrapperFilter qwf = new QueryWrapperFilter(createTagQuery(tags));
		FilteredQuery fq = new FilteredQuery(mltQuery, qwf);

		return fq;
	}

	public static int getDocId(IndexSearcher is, String postId) throws IOException {
		Query query = new TermQuery(new Term(Indexer.FIELD_ID, postId));
		TopDocs topDoc = is.search(query, 1);

		return topDoc.scoreDocs[0].doc;
	}

	public static String getDocWithLowestId(IndexSearcher is) throws IOException {
		Sort sort = new Sort(new SortField(Indexer.FIELD_ID, SortField.Type.INT, false));
		Query madQuery = new MatchAllDocsQuery();
		TopDocs topDocs = is.search(madQuery, 1, sort);
		int docId = topDocs.scoreDocs[0].doc;
		Document document = is.doc(docId);
		String postId = document.get(Indexer.FIELD_ID);

		return postId;
	}

	public static boolean doesDocExist(IndexSearcher is, String postId) throws IOException {
		boolean doesExist = false;

		Query query = new TermQuery(new Term(Indexer.FIELD_ID, postId));
		TopDocs topDocs = is.search(query, 1);

		if (topDocs.totalHits > 0) {
			doesExist = true;
		}

		return doesExist;
	}
}
