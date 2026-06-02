package com.gvr.notes.pdf;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gvr.notes.model.Topic;
import lombok.extern.slf4j.Slf4j;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
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
}