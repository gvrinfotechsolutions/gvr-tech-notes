package com.gvr.notes.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
public class MarkdownService {

    public String convertMarkdownToHtml(String markdown) {

        Parser parser = Parser.builder().build();

        Node document = parser.parse(markdown);

        HtmlRenderer renderer =
                HtmlRenderer.builder().build();

        return renderer.render(document);
    }
}