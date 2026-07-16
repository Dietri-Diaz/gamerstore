package com.gamerstore.app.service;

import com.gamerstore.app.model.Pedido;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Genera el PDF del reporte de pedidos con OpenPDF. */
@Service
public class PedidoReporteService {

    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color HEAD_BG = new Color(30, 27, 75);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Arma el PDF del reporte con OpenPDF: título, línea de filtros aplicados, tabla de pedidos
     * (encabezado + una fila por pedido) y pie con el total de ventas y la cantidad de pedidos.
     */
    public byte[] generar(List<Pedido> pedidos, LocalDate desde, LocalDate hasta, String estado) {
        Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ACCENT);
            doc.add(new Paragraph("GamerStore — Reporte de Pedidos", titulo));

            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            StringBuilder filtros = new StringBuilder("Filtros: ");
            filtros.append(desde != null ? "desde " + desde.format(FMT) + " " : "");
            filtros.append(hasta != null ? "hasta " + hasta.format(FMT) + " " : "");
            filtros.append(estado != null && !estado.isBlank() ? "estado " + estado : "");
            if (desde == null && hasta == null && (estado == null || estado.isBlank())) filtros.append("todos");
            doc.add(new Paragraph(filtros.toString().trim(), sub));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{2f, 3.2f, 2f, 2.2f, 2f, 1.4f, 2.2f});
            table.setWidthPercentage(100);
            String[] cols = {"Código", "Cliente", "Fecha", "Estado", "Método", "Ítems", "Total (S/)"};
            Font th = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String c : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(c, th));
                cell.setBackgroundColor(HEAD_BG);
                cell.setPadding(6);
                cell.setBorderColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            Font td = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            double totalVentas = 0;
            for (Pedido p : pedidos) {
                addCell(table, p.getCodigo(), td, false);
                addCell(table, p.getCliente() != null ? p.getCliente().getNombreCompleto() : "—", td, false);
                addCell(table, p.getFecha() != null ? p.getFecha().toLocalDate().format(FMT) : "—", td, false);
                addCell(table, p.getEstado(), td, false);
                addCell(table, p.getMetodoPago() != null ? p.getMetodoPago() : "—", td, false);
                addCell(table, String.valueOf(p.getCantidadTotal()), td, false);
                addCell(table, String.format("%,.2f", p.getTotal()), td, true);
                totalVentas += p.getTotal();
            }
            doc.add(table);
            doc.add(Chunk.NEWLINE);

            Font pie = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEAD_BG);
            doc.add(new Paragraph(
                    "Total ventas: S/ " + String.format("%,.2f", totalVentas) + "   |   "
                            + pedidos.size() + " pedidos", pie));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF", e);
        }
    }

    // Crea una celda de la tabla con el texto y la fuente dados; si right es true, alinea a la derecha (montos).
    private void addCell(PdfPTable table, String text, Font font, boolean right) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        if (right) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
