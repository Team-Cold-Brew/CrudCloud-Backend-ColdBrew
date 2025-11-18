package com.riwi.CrudCloud.database.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import com.riwi.CrudCloud.common.models.Database; // ¡CAMBIO! Importamos la nueva entidad Database
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfGeneratorService {

    /**
     * Generates a PDF with the database credentials (including the RAW password).
     * @param database The database entity.
     * @param rawPassword The password in plain text (only displayed once).
     * @return Array of bytes from the PDF.
     */
    public byte[] generateInstanceCredentialsPdf(Database database, String rawPassword) { // CAMBIO de Instance a Database

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, os);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Paragraph title = new Paragraph("Access Credentials - CrudCloud", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(255, 0, 0));
            Paragraph warning = new Paragraph("SAFETY WARNING!", warningFont);
            warning.setSpacingAfter(5);
            document.add(warning);
            document.add(new Paragraph("This is the ONLY time you will see the password in plain text.", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            document.add(new Paragraph("Keep this document in a safe place.", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            document.add(new Paragraph("\n"));


            Table table = new Table(2);
            table.setWidth(100);
            table.setPadding(5);

            Runnable addRow = () -> {
                table.addCell(createCell("Instance Name:", true));
                table.addCell(createCell(database.getName(), false));
                table.addCell(createCell("Database Type:", true));
                table.addCell(createCell(database.getDbType().name(), false));
                table.addCell(createCell("Host/Direction:", true));
                table.addCell(createCell(database.getHost(), false));
                table.addCell(createCell("Port:", true));
                table.addCell(createCell(String.valueOf(database.getPort()), false));
                table.addCell(createCell("User:", true));
                table.addCell(createCell(database.getUsername(), false));

                table.addCell(createCell("Password (RAW):", true, new Color(0, 100, 0)));
                table.addCell(createCell(rawPassword, false, new Color(0, 100, 0)));

                table.addCell(createCell("Date of Creation:", true));
                table.addCell(createCell(database.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), false));
            };

            addRow.run();
            document.add(table);

        } catch (DocumentException e) {
            log.error("Error generating the credentials PDF: {}", e.getMessage());
            throw new RuntimeException("Error generating PDF.", e);
        } finally {
            document.close();
        }

        return os.toByteArray();
    }

    // Auxiliary method for creating table cells
    private Cell createCell(String text, boolean isHeader) throws BadElementException {
        return createCell(text, isHeader, new Color(0, 0, 0));
    }

    private Cell createCell(String text, boolean isHeader, Color color) throws BadElementException {
        Font font = FontFactory.getFont(isHeader ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10, color);
        Cell cell = new Cell(new Phrase(text, font));
        cell.setHorizontalAlignment(isHeader ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        cell.setBackgroundColor(isHeader ? new Color(240, 240, 240) : Color.WHITE);
        cell.setBorderColor(new Color(200, 200, 200));
        return cell;
    }
}