package com.gamerstore.app.service;

import com.gamerstore.app.model.Pago;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Genera el PDF del comprobante de pago con OpenPDF. */
@Service
public class PagoComprobanteService {

    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color HEAD_BG = new Color(30, 27, 75);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Arma el PDF del comprobante con OpenPDF: título, datos del pago y del pedido
     * (tabla clave/valor) y el monto destacado en grande al final.
     */
    public byte[] generar(Pago pago) {
        Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ACCENT);
            doc.add(new Paragraph("GamerStore — Comprobante de Pago", titulo));

            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            doc.add(new Paragraph("Código: " + pago.getCodigo(), sub));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{2.4f, 4f});
            table.setWidthPercentage(100);

            Font lbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font val = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            addFila(table, "Fecha", pago.getFecha() != null ? pago.getFecha().format(FMT) : "—", lbl, val);
            addFila(table, "Cliente",
                    (pago.getPedido() != null && pago.getPedido().getCliente() != null)
                            ? pago.getPedido().getCliente().getNombreCompleto() : "—", lbl, val);
            addFila(table, "Pedido", pago.getPedido() != null ? pago.getPedido().getCodigo() : "—", lbl, val);
            addFila(table, "Método", pago.getMetodo(), lbl, val);
            addFila(table, "Referencia", pago.getReferencia(), lbl, val);
            if (pago.getTarjetaUlt4() != null && !pago.getTarjetaUlt4().isBlank()) {
                addFila(table, "Tarjeta", "**** " + pago.getTarjetaUlt4(), lbl, val);
            }
            addFila(table, "Estado", pago.getEstado(), lbl, val);

            doc.add(table);
            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            Font montoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, HEAD_BG);
            Paragraph monto = new Paragraph("MONTO: S/ " + String.format("%,.2f", pago.getMonto()), montoFont);
            monto.setAlignment(Element.ALIGN_RIGHT);
            doc.add(monto);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF", e);
        }
    }

    // Agrega una fila "etiqueta / valor" a la tabla del comprobante.
    private void addFila(PdfPTable table, String etiqueta, String valor, Font lbl, Font val) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, lbl));
        c1.setBackgroundColor(HEAD_BG);
        c1.setPadding(6);
        c1.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "—", val));
        c2.setPadding(6);
        c2.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(c2);
    }
}
