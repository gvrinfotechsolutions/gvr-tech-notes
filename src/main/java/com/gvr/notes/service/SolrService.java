
package com.gvr.notes.service;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gvr.notes.model.Topic;
import com.gvr.notes.repository.TopicRepository;

import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

@Service
public class SolrService {

	@Autowired
	private TopicRepository topicRepository;

	private final SolrClient solrClient;

	public SolrService(SolrClient solrClient) {
		this.solrClient = solrClient;
	}

	public void reindexAllTopics() throws Exception {

		System.out.println("===== SOLR REINDEX STARTED =====");

		solrClient.deleteByQuery("*:*");
		solrClient.commit();

		List<Topic> topics = topicRepository.findAll();

		System.out.println("Topics Found = " + topics.size());

		for (Topic topic : topics) {

			indexTopic(topic);

			System.out.println("Indexed Topic : " + topic.getTitle());
		}

		System.out.println("===== SOLR REINDEX COMPLETED =====");
	}

	public void testIndex() throws Exception {

		SolrInputDocument doc = new SolrInputDocument();

		doc.addField("id", "1");

		doc.addField("title", "Java Streams");

		doc.addField("content", "Java Stream API Tutorial");

		solrClient.add(doc);

		solrClient.commit();
	}

	public void indexTopic(Topic topic) throws Exception {

		System.out.println("===== INDEXING TO SOLR =====");
		System.out.println("ID      : " + topic.getId());
		System.out.println("TITLE   : " + topic.getTitle());

		SolrInputDocument doc = new SolrInputDocument();

		doc.addField("id", topic.getId());
		doc.addField("title", topic.getTitle());
		doc.addField("content", topic.getContent());
		doc.addField("tags", topic.getTags());

		if (topic.getSubject() != null) {
			doc.addField("subject", topic.getSubject().getName());
		}

		solrClient.add(doc);
		solrClient.commit();

		System.out.println("===== INDEXED SUCCESSFULLY =====");
	}

	public List<String> searchTitles(String keyword) throws Exception {

		SolrQuery query = new SolrQuery();

		query.setQuery("title:*" + keyword + "* OR " + "content:*" + keyword + "* OR " + "tags:*" + keyword + "* OR "
				+ "subject:*" + keyword + "*");

		QueryResponse response = solrClient.query(query);

		List<String> results = new ArrayList<>();

		for (SolrDocument doc : response.getResults()) {

			results.add(String.valueOf(doc.getFieldValue("title")));
		}

		return results;
	}

	public void deleteTopic(Long topicId) throws Exception {

		solrClient.deleteById(topicId.toString());

		solrClient.commit();
	}

	public List<Long> searchTopicIds(String keyword) throws Exception {

		SolrQuery query = new SolrQuery();

		query.setQuery("title:(\"" + keyword + "\") OR " + "content:(\"" + keyword + "\") OR " + "tags:(\"" + keyword
				+ "\") OR " + "subject:(\"" + keyword + "\")");

		QueryResponse response = solrClient.query(query);

		List<Long> topicIds = new ArrayList<>();

		for (SolrDocument doc : response.getResults()) {

			topicIds.add(Long.valueOf(doc.getFieldValue("id").toString()));
		}

		return topicIds;
	}

}