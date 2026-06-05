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

		// Step 1: Collapse 3+ consecutive blank lines to one.
		String normalised = markdown.replaceAll("(?m)(\\r?\\n){3,}", "\n\n");

		// Step 2: Convert loose lists → tight lists.
		// AI content puts a blank line between every list item, e.g.:
		//   1. First item\n\n2. Second item\n\n3. Third item
		// Flexmark treats that as a "loose list" and wraps each <li> in a <p>,
		// producing large gaps between items. Removing the blank line between
		// consecutive list items makes the list "tight" — no <p> wrappers, no gaps.
		normalised = normalised.replaceAll(
			"(?m)(^[ \\t]*(?:[-*+]|\\d+[.)]) [^\\n\\r]+)[ \\t]*\\r?\\n[ \\t]*\\r?\\n(?=[ \\t]*(?:[-*+]|\\d+[.)]) )",
			"$1\n"
		);

		Node document = parser.parse(normalised);
		return renderer.render(document);
	}
}
