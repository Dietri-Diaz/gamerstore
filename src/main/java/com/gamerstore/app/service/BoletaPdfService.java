package com.gamerstore.app.service;

import com.gamerstore.app.model.Comprobante;
import com.gamerstore.app.model.PedidoItem;
import com.gamerstore.app.util.NumeroALetras;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el PDF de la boleta de venta electrónica a partir de una plantilla HTML/CSS
 * (Thymeleaf) renderizada a PDF con openhtmltopdf. El diseño (colores, tabla de ítems,
 * recuadro del comprobante, QR, etc.) vive en templates/boleta.html; esta clase solo
 * arma los datos y hace la conversión HTML -> PDF.
 *
 * ADVERTENCIA (léase también el pie del PDF): esta boleta es una DEMOSTRACIÓN académica.
 * No se transmite a SUNAT (haría falta RUC real y certificado digital), así que no tiene
 * validez tributaria.
 */
@Service
public class BoletaPdfService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_QR_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TemplateEngine templateEngine;
    private final String razonSocial;
    private final String ruc;
    private final String direccionEmpresa;

    public BoletaPdfService(TemplateEngine templateEngine,
                            @Value("${app.empresa.razon-social}") String razonSocial,
                            @Value("${app.empresa.ruc}") String ruc,
                            @Value("${app.empresa.direccion}") String direccionEmpresa) {
        this.templateEngine = templateEngine;
        this.razonSocial = razonSocial;
        this.ruc = ruc;
        this.direccionEmpresa = direccionEmpresa;
    }

    /** Arma el PDF de la boleta (comprobante + ítems del pedido) renderizando la plantilla boleta.html. */
    public byte[] generar(Comprobante c, List<PedidoItem> items) {
        try {
            Context ctx = new Context();
            ctx.setVariable("empresa", Map.of(
                    "razonSocial", razonSocial,
                    "ruc", ruc,
                    "direccion", direccionEmpresa));
            ctx.setVariable("c", c);
            ctx.setVariable("items", items);
            ctx.setVariable("enLetras", NumeroALetras.convertir(c.getTotal()));
            ctx.setVariable("qr", generarQrDataUri(textoQr(c)));
            ctx.setVariable("fechaTexto", c.getFechaEmision() != null ? c.getFechaEmision().format(FMT_FECHA) : "—");

            String html = templateEngine.process("boleta", ctx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar la boleta", e);
        }
    }

    // Arma el texto del QR con el formato usado por SUNAT para boletas (03 = boleta, 1 = DNI).
    private String textoQr(Comprobante c) {
        String fecha = c.getFechaEmision() != null ? c.getFechaEmision().toLocalDate().format(FMT_QR_FECHA) : "";
        String dni = (c.getClienteDni() == null || c.getClienteDni().isBlank()) ? "—" : c.getClienteDni();
        return ruc + "|03|" + c.getSerie() + "|" + c.getNumero() + "|"
                + String.format("%.2f", c.getIgv()) + "|" + String.format("%.2f", c.getTotal()) + "|"
                + fecha + "|1|" + dni + "|";
    }

    /**
     * Genera la imagen del QR con ZXing y la devuelve como data URI ("data:image/png;base64,...")
     * lista para usarse directamente en el atributo src de un <img/> de la plantilla.
     */
    private String generarQrDataUri(String contenido) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 220, 220, hints);

        BufferedImage bufferedImage = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                bufferedImage.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    }
}
