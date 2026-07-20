package com.gamerstore.app.util;

/**
 * Convierte un importe (double) a su representación en letras, con el formato
 * usado en las boletas de venta peruanas: "SON: MIL NOVENTA Y OCHO CON 50/100 SOLES".
 * Soporta hasta 999,999,999 (no se necesitan "billones" para un monto de venta).
 */
public final class NumeroALetras {

    private NumeroALetras() {}

    // Unidades 0-29: incluye las excepciones de los "teens" (ONCE..DIECINUEVE) y los
    // veinte-tantos escritos como una sola palabra (VEINTIUNO, VEINTIDOS, etc.).
    private static final String[] UNIDADES = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
            "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO",
            "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };

    // Decenas de 30 a 90 (a partir de aquí ya se arman con "Y", p. ej. "TREINTA Y UNO").
    private static final String[] DECENAS = {
            "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    // Centenas de 100 (CIENTO, no "CIEN") a 900. El caso exacto "100" se resuelve aparte como "CIEN".
    private static final String[] CENTENAS = {
            "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    /**
     * Convierte el monto a letras con formato de boleta: "SON: <IMPORTE> CON XX/100 SOLES".
     * Redondea a centavos (2 decimales) antes de separar la parte entera de los centavos,
     * para no arrastrar errores de coma flotante (p. ej. 21.00 -> 2100 centavos exactos).
     */
    public static String convertir(double monto) {
        long centavosTotales = Math.round(Math.max(monto, 0) * 100);
        long entero = centavosTotales / 100;
        int centavos = (int) (centavosTotales % 100);

        String letras = numeroEnteroALetras(entero);
        return "SON: " + letras + " CON " + String.format("%02d", centavos) + "/100 SOLES";
    }

    // Convierte la parte entera (soles) a letras, separando millones, miles y el grupo final (0-999).
    private static String numeroEnteroALetras(long n) {
        if (n == 0) return "CERO";

        long millones = n / 1_000_000;
        long restoMillones = n % 1_000_000;
        long miles = restoMillones / 1000;
        long unidadesFinal = restoMillones % 1000;

        StringBuilder sb = new StringBuilder();

        if (millones > 0) {
            // "UN MILLON" (nunca "UNO MILLON"), pero "DOS MILLONES", "VEINTIUN MILLONES", etc.
            if (millones == 1) {
                sb.append("UN MILLON");
            } else {
                sb.append(apocopar(grupo((int) millones))).append(" MILLONES");
            }
        }

        if (miles > 0) {
            if (sb.length() > 0) sb.append(" ");
            // Caso especial del español: 1000 es "MIL" (nunca "UN MIL"). De 2000 en adelante
            // sí se antepone el número, apocopando el "UNO" final cuando corresponde
            // (p. ej. 21000 -> "VEINTIUN MIL", no "VEINTIUNO MIL").
            if (miles == 1) {
                sb.append("MIL");
            } else {
                sb.append(apocopar(grupo((int) miles))).append(" MIL");
            }
        }

        if (unidadesFinal > 0) {
            if (sb.length() > 0) sb.append(" ");
            // El último grupo va sin apocopar: si el monto termina en 1 (p. ej. 21), se
            // escribe "VEINTIUNO" completo porque no antecede a ningún sustantivo (sigue "CON").
            sb.append(grupo((int) unidadesFinal));
        }

        return sb.toString();
    }

    // Convierte un número de 0 a 999 a letras (sin mil ni millones).
    private static String grupo(int n) {
        if (n == 0) return "";
        if (n == 100) return "CIEN"; // exacto: "CIEN", no "CIENTO"

        StringBuilder sb = new StringBuilder();
        int centena = n / 100;
        int resto = n % 100;

        if (centena > 0) {
            sb.append(CENTENAS[centena - 1]);
        }
        if (resto > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(decenasYUnidades(resto));
        }
        return sb.toString();
    }

    // Convierte un número de 1 a 99 a letras, uniendo decena y unidad con "Y" cuando corresponde.
    private static String decenasYUnidades(int n) {
        if (n < 30) return UNIDADES[n];

        int decena = n / 10;
        int unidad = n % 10;
        String base = DECENAS[decena - 3];
        return unidad == 0 ? base : base + " Y " + UNIDADES[unidad];
    }

    // Apocopa el "UNO" final a "UN" cuando el número antecede a un sustantivo (MIL, MILLONES).
    // Ej: "VEINTIUNO" -> "VEINTIUN", "TREINTA Y UNO" -> "TREINTA Y UN".
    private static String apocopar(String palabras) {
        return palabras.endsWith("UNO") ? palabras.substring(0, palabras.length() - 1) : palabras;
    }
}
