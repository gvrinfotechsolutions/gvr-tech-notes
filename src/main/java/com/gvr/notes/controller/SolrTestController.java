package com.gvr.notes.controller;

import com.gvr.notes.service.SolrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
public class SolrTestController {

    private final SolrService solrService;

    public SolrTestController(
            SolrService solrService) {

        this.solrService = solrService;
    }

    @GetMapping("/solr-test")
    public String test() throws Exception {

        solrService.testIndex();

        return "Indexed Successfully";
    }
    
    @GetMapping("/solr-search")
    @ResponseBody
    public List<String> search(
            @RequestParam String keyword)
            throws Exception {

        return solrService.searchTitles(keyword);
    }
}