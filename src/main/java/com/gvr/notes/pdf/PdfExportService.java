package com.gvr.notes.pdf;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gvr.notes.model.Subject;
import com.gvr.notes.model.Topic;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.FontFactory;

@Service
@Slf4j
public class PdfExportService {

	public ByteArrayInputStream exportTopicsPdf(

			Map<String, List<Topic>> groupedTopics) {

		Document document = new Document();

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {

			// PDF WRITER

			PdfWriter writer = PdfWriter.getInstance(document, out);

			writer.setPageEvent(
			        new WatermarkPageEvent());
			document.open();

			// WATERMARK


			// FONTS

			Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(0, 102, 51));

			Font subTitleFont = new Font(Font.HELVETICA, 18, Font.NORMAL);

			Font subjectFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(0, 51, 102));

			Font topicFont = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0, 128, 0));

			Font contentFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(60, 60, 60));

			Font codeFont = FontFactory.getFont(FontFactory.COURIER, 11, Font.NORMAL, new Color(0, 0, 153));
			// =========================
			// COVER PAGE
			// =========================

			document.add(new Paragraph("\n\n\n"));

			Paragraph title = new Paragraph("GVR-Tech-Notes", titleFont);

			title.setAlignment(Paragraph.ALIGN_CENTER);

			title.setSpacingAfter(25);

			document.add(title);

			Paragraph subtitle = new Paragraph("Developer Knowledge Backup", subTitleFont);

			subtitle.setAlignment(Paragraph.ALIGN_CENTER);

			document.add(subtitle);

			// NEW PAGE

			document.newPage();

			// =========================
			// SUBJECTS + TOPICS
			// =========================

			for (Map.Entry<String, List<Topic>> entry : groupedTopics.entrySet()) {

				// SUBJECT DIVIDER

				Paragraph divider = new Paragraph("━━━━━━━━━━━━━━━━━━━━", subjectFont);

				divider.setSpacingBefore(10);

				document.add(divider);

				// SUBJECT NAME

				Paragraph subject = new Paragraph(entry.getKey().toUpperCase(), subjectFont);

				subject.setSpacingBefore(2);

				subject.setSpacingAfter(4);

				document.add(subject);

				Paragraph divider2 = new Paragraph("━━━━━━━━━━━━━━━━━━━━", subjectFont);

				divider2.setSpacingAfter(15);

				document.add(divider2);

				// TOPICS

				for (Topic topic : entry.getValue()) {

					// TOPIC TITLE

					Paragraph topicTitle = new Paragraph("▶ " + topic.getTitle(), topicFont);

					topicTitle.setSpacingBefore(5);

					topicTitle.setSpacingAfter(3);

					document.add(topicTitle);

					// TOPIC CONTENT

					// CLEAN CONTENT

					String content = topic.getContent();

					String[] lines = content.split("\n");

					boolean codeBlock = false;

					for (String line : lines) {

						// START / END CODE BLOCK

						if (line.trim().startsWith("```")) {

							codeBlock = !codeBlock;

							continue;
						}

						// REMOVE MARKDOWN HEADINGS

						line = line.replace("##", "").replace("---", "");

						// CODE CONTENT

						// SECTION HEADINGS

						if (line.endsWith(":")) {

							Font headingFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(153, 0, 76));

							Paragraph heading = new Paragraph(line, headingFont);

							heading.setSpacingBefore(4);

							heading.setSpacingAfter(2);

							document.add(heading);

							continue;
						}

						// NORMAL CONTENT

						else {

							line = line.trim();

							// SKIP EMPTY LINES

							if (line.isEmpty()) {

								continue;
							}

							Paragraph normalParagraph = new Paragraph(line + " ", contentFont);

							normalParagraph.setLeading(14);

							normalParagraph.setSpacingAfter(1);

							document.add(normalParagraph);
						}
					}

					document.add(new Paragraph("\n"));
				}
			}

			document.close();

		} catch (Exception e) {
			log.error("PDF generation failed", e);
			throw new RuntimeException("PDF generation failed", e);
		}

		return new ByteArrayInputStream(out.toByteArray());
	}

	// ─────────────────────────────────────────────────────────────────────────
	// SUBJECT BULK EXPORT — one subject, table of contents, better formatting
	// ─────────────────────────────────────────────────────────────────────────

	public ByteArrayInputStream exportSubjectPdf(Subject subject, List<Topic> topics) {

		Document document = new Document();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			PdfWriter writer = PdfWriter.getInstance(document, out);
			writer.setPageEvent(new WatermarkPageEvent());
			document.open();

			// ── FONTS ──
			Font titleFont    = new Font(Font.HELVETICA, 26, Font.BOLD,   new Color(0,  102, 51));
			Font subTitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, new Color(80, 80,  80));
			Font tocTitleFont = new Font(Font.HELVETICA, 16, Font.BOLD,   new Color(0,  51, 102));
			Font tocItemFont  = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(30, 30,  30));
			Font topicFont    = new Font(Font.HELVETICA, 14, Font.BOLD,   new Color(0, 128,   0));
			Font headingFont  = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(153, 0,  76));
			Font contentFont  = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(60, 60,  60));
			Font codeFont     = FontFactory.getFont(FontFactory.COURIER, 9, Font.NORMAL, new Color(0, 0, 153));
			Font metaFont     = new Font(Font.HELVETICA, 9,  Font.ITALIC, new Color(120, 120, 120));

			// ── COVER PAGE ──
			document.add(new Paragraph("\n\n\n\n"));

			Paragraph title = new Paragraph("GVR Tech Notes", titleFont);
			title.setAlignment(Paragraph.ALIGN_CENTER);
			title.setSpacingAfter(10);
			document.add(title);

			Paragraph subjectTitle = new Paragraph(subject.getName(), tocTitleFont);
			subjectTitle.setAlignment(Paragraph.ALIGN_CENTER);
			subjectTitle.setSpacingAfter(8);
			document.add(subjectTitle);

			if (subject.getDescription() != null && !subject.getDescription().isBlank()) {
				Paragraph desc = new Paragraph(subject.getDescription(), subTitleFont);
				desc.setAlignment(Paragraph.ALIGN_CENTER);
				desc.setSpacingAfter(6);
				document.add(desc);
			}

			String exportedOn = "Exported: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
			Paragraph meta = new Paragraph(exportedOn + "  |  " + topics.size() + " topics", metaFont);
			meta.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(meta);

			document.newPage();

			// ── TABLE OF CONTENTS ──
			Paragraph tocTitle = new Paragraph("Table of Contents", tocTitleFont);
			tocTitle.setSpacingAfter(14);
			document.add(tocTitle);

			PdfPTable tocTable = new PdfPTable(2);
			tocTable.setWidthPercentage(100);
			tocTable.setWidths(new float[]{0.6f, 0.4f});
			tocTable.setSpacingAfter(20);

			// header row
			PdfPCell hTopic = new PdfPCell(new Phrase("Topic", headingFont));
			hTopic.setBorder(Rectangle.BOTTOM);
			hTopic.setPadding(6);
			PdfPCell hTags = new PdfPCell(new Phrase("Tags", headingFont));
			hTags.setBorder(Rectangle.BOTTOM);
			hTags.setPadding(6);
			tocTable.addCell(hTopic);
			tocTable.addCell(hTags);

			for (int i = 0; i < topics.size(); i++) {
				Topic t = topics.get(i);
				PdfPCell numCell = new PdfPCell(new Phrase((i + 1) + ". " + t.getTitle(), tocItemFont));
				numCell.setBorder(Rectangle.NO_BORDER);
				numCell.setPadding(5);
				PdfPCell tagCell = new PdfPCell(new Phrase(
					t.getTags() != null ? t.getTags() : "—", metaFont));
				tagCell.setBorder(Rectangle.NO_BORDER);
				tagCell.setPadding(5);
				tocTable.addCell(numCell);
				tocTable.addCell(tagCell);
			}
			document.add(tocTable);
			document.newPage();

			// ── TOPICS ──
			for (int i = 0; i < topics.size(); i++) {
				Topic topic = topics.get(i);

				// topic title
				Paragraph topicTitle = new Paragraph((i + 1) + ". " + topic.getTitle(), topicFont);
				topicTitle.setSpacingBefore(8);
				topicTitle.setSpacingAfter(4);
				document.add(topicTitle);

				// meta row: tags, difficulty, views
				StringBuilder metaLine = new StringBuilder();
				if (topic.getDifficulty() != null)
					metaLine.append("Difficulty: ").append(topic.getDifficulty()).append("  |  ");
				if (topic.getTags() != null && !topic.getTags().isBlank())
					metaLine.append("Tags: ").append(topic.getTags()).append("  |  ");
				metaLine.append("Views: ").append(topic.getViewCount());
				document.add(new Paragraph(metaLine.toString(), metaFont));
				document.add(new Paragraph("\n"));

				// content
				renderMarkdownContent(document, topic.getContent(), contentFont, headingFont, codeFont);

				// code snippet
				if (topic.getCodeSnippet() != null && !topic.getCodeSnippet().isBlank()) {
					document.add(new Paragraph("\n"));
					Font codeHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(0, 0, 153));
					document.add(new Paragraph("Code Snippet (" + (topic.getCodeLanguage() != null ? topic.getCodeLanguage() : "code") + "):", codeHeaderFont));
					for (String line : topic.getCodeSnippet().split("\n")) {
						Paragraph cp = new Paragraph(line.isEmpty() ? " " : line, codeFont);
						cp.setLeading(13);
						document.add(cp);
					}
				}

				document.add(new Paragraph("\n"));
				document.add(new Paragraph("─────────────────────────────────────────────────────────────",
					new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(200, 200, 200))));
				document.add(new Paragraph("\n"));
			}

			document.close();

		} catch (Exception e) {
			log.error("Subject PDF generation failed. subject={}", subject.getName(), e);
			throw new RuntimeException("Subject PDF generation failed", e);
		}

		return new ByteArrayInputStream(out.toByteArray());
	}

	/** Render markdown content lines with basic styling */
	private void renderMarkdownContent(Document document, String content,
	                                    Font contentFont, Font headingFont, Font codeFont) throws Exception {
		if (content == null || content.isBlank()) return;

		String[] lines = content.split("\n");
		boolean inCodeBlock = false;

		for (String line : lines) {
			if (line.trim().startsWith("```")) {
				inCodeBlock = !inCodeBlock;
				continue;
			}

			if (inCodeBlock) {
				Paragraph cp = new Paragraph(line.isEmpty() ? " " : line, codeFont);
				cp.setLeading(13);
				document.add(cp);
				continue;
			}

			// strip markdown heading markers
			String stripped = line
				.replaceAll("^#+\\s*", "")
				.replaceAll("^>\\s*", "")
				.replaceAll("---+", "")
				.replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
				.replaceAll("\\*([^*]+)\\*", "$1")
				.replaceAll("`([^`]+)`", "$1")
				.trim();

			if (stripped.isEmpty()) continue;

			// detect heading
			boolean isHeading = line.trim().startsWith("#") ||
				(stripped.length() < 80 && stripped.endsWith(":") && !stripped.contains(" ") == false
					&& line.trim().startsWith("##"));

			if (line.trim().startsWith("##") || line.trim().startsWith("# ")) {
				Paragraph h = new Paragraph(stripped, headingFont);
				h.setSpacingBefore(8);
				h.setSpacingAfter(3);
				document.add(h);
			} else {
				Paragraph p = new Paragraph(stripped, contentFont);
				p.setLeading(14);
				p.setSpacingAfter(1);
				document.add(p);
			}
		}
	}
}