package com.cabex.backend.rapport.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RapportExporter {

    public byte[] exporter(RapportData data, String format) {
        String f = format == null ? "csv" : format.toLowerCase();
        return switch (f) {
            case "xlsx", "excel" -> excel(data);
            case "pdf" -> pdf(data);
            default -> csv(data);
        };
    }

    public String contentType(String format) {
        String f = format == null ? "csv" : format.toLowerCase();
        return switch (f) {
            case "xlsx", "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> "text/csv";
        };
    }

    public String extension(String format) {
        String f = format == null ? "csv" : format.toLowerCase();
        return switch (f) {
            case "xlsx", "excel" -> "xlsx";
            case "pdf" -> "pdf";
            default -> "csv";
        };
    }

    private byte[] csv(RapportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", data.enTetes())).append('\n');
        for (List<Object> ligne : data.lignes()) {
            sb.append(ligne.stream()
                    .map(v -> v == null ? "" : "\"" + String.valueOf(v).replace("\"", "\"\"") + "\"")
                    .collect(Collectors.joining(";")));
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(RapportData data) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(abbrege(data.titre()));
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < data.enTetes().size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(data.enTetes().get(i));
                cell.setCellStyle(headerStyle);
            }
            int r = 1;
            for (List<Object> ligne : data.lignes()) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < ligne.size(); c++) {
                    Object v = ligne.get(c);
                    Cell cell = row.createCell(c);
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(v == null ? "" : String.valueOf(v));
                    }
                }
            }
            for (int i = 0; i < data.enTetes().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'export Excel", e);
        }
    }

    private byte[] pdf(RapportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(data.titre(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(data.enTetes().size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            for (String entete : data.enTetes()) {
                PdfPCell cell = new PdfPCell(new Phrase(entete, headerFont));
                cell.setBackgroundColor(new java.awt.Color(64, 64, 64));
                table.addCell(cell);
            }
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (List<Object> ligne : data.lignes()) {
                for (Object v : ligne) {
                    table.addCell(new Phrase(v == null ? "" : String.valueOf(v), cellFont));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'export PDF", e);
        }
    }

    private String abbrege(String titre) {
        String t = titre == null ? "rapport" : titre.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        return t.isEmpty() ? "rapport" : (t.length() > 28 ? t.substring(0, 28) : t);
    }
}
