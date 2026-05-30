package com.gvr.notes.solr;

import org.apache.solr.client.solrj.beans.Field;

public class TopicSolrDocument {

    @Field
    private String id;

    @Field
    private String title;

    @Field
    private String content;

    @Field
    private String tags;

    @Field
    private String subject;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}