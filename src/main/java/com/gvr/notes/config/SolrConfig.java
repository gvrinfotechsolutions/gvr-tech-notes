package com.gvr.notes.config;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

@Configuration
public class SolrConfig {

    @Value("${solr.url}")
    private String solrUrl;

    @Value("${solr.core}")
    private String core;

      
    
    @Bean
    public SolrClient solrClient() {

        return new HttpSolrClient.Builder(solrUrl + "/" + core)
                .build();
    }
}