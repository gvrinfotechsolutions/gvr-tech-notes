package com.gvr.notes.pdf;

import java.awt.Color;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class WatermarkPageEvent extends PdfPageEventHelper {

	@Override
	public void onEndPage(

			PdfWriter writer,

			com.lowagie.text.Document document) {

		try {

			PdfContentByte canvas = writer.getDirectContentUnder();

			BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);

			canvas.saveState();

			canvas.beginText();

			canvas.setColorFill(new Color(240, 240, 240));

			canvas.setFontAndSize(baseFont, 42);

			canvas.showTextAligned(PdfContentByte.ALIGN_CENTER,

					"GVR-Tech-Notes © Venky",

					320,

					450,

					35);

			canvas.endText();

			canvas.restoreState();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}