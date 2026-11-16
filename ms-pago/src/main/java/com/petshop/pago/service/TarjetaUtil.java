package com.petshop.pago.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.HexFormat;

/** Utilidades para tratar el numero de tarjeta sin guardarlo ni mostrarlo. */
public final class TarjetaUtil {

    private TarjetaUtil() {
    }

    /** Deja solo los digitos (se aceptan espacios y guiones al escribir la tarjeta). */
    public static String normalizar(String numero) {
        return numero == null ? "" : numero.replaceAll("\\D", "");
    }

    public static String ultimos4(String digitos) {
        return digitos.length() <= 4 ? digitos : digitos.substring(digitos.length() - 4);
    }

    /** Algoritmo de Luhn: detecta errores de digitacion en el numero de tarjeta. */
    public static boolean luhnValido(String digitos) {
        if (digitos == null || digitos.length() < 12 || digitos.length() > 19) {
            return false;
        }
        int suma = 0;
        boolean duplicar = false;
        for (int i = digitos.length() - 1; i >= 0; i--) {
            int d = digitos.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            if (duplicar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            suma += d;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    /** SHA-256(pepper + numero) en hexadecimal: identifica la tarjeta sin poder recuperar el numero. */
    public static String hash(String digitos, String pepper) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest((pepper + digitos).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /** "MM/AA": vence al terminar ese mes. */
    public static boolean vencida(String vencimiento, YearMonth hoy) {
        String[] partes = vencimiento.split("/");
        YearMonth vence = YearMonth.of(2000 + Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
        return vence.isBefore(hoy);
    }
}
