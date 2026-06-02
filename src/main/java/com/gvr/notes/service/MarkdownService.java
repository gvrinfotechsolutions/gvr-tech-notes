package com.gvr.notes.service;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class MarkdownService {

	private final Parser parser;
	private final HtmlRenderer renderer;

	public MarkdownService() {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(
			TablesExtension.create(),
			StrikethroughExtension.create(),
			TaskListExtension.create(),
			AutolinkExtension.create()
		));
		options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
		options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
		options.set(HtmlRenderer.RENDER_HEADER_ID, true);
		this.parser = Parser.builder(options).build();
		this.renderer = HtmlRenderer.builder(options).build();
	}

	public String convertMarkdownToHtml(String markdown) {
		if (markdown == null || markdown.isBlank())
			return "";
		Node document = parser.parse(markdown);
		return renderer.render(document);
	}
}
