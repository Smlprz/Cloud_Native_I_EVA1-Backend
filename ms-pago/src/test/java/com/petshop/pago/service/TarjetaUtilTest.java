package com.petshop.pago.service;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TarjetaUtilTest {

    @Test
    void normalizaDejandoSoloDigitos() {
        assertEquals("4111111111111111", TarjetaUtil.normalizar("4111 1111-1111 1111"));
        assertEquals("", TarjetaUtil.normalizar(null));
    }

    @Test
    void validaElAlgoritmoDeLuhn() {
        assertTrue(TarjetaUtil.luhnValido("4111111111111111"));
        assertTrue(TarjetaUtil.luhnValido("5555555555554444"));
        assertTrue(TarjetaUtil.luhnValido("378282246310005"));
        assertFalse(TarjetaUtil.luhnValido("4111111111111112"));
        assertFalse(TarjetaUtil.luhnValido("1234"));
        assertFalse(TarjetaUtil.luhnValido(null));
    }

    @Test
    void ultimosCuatroDigitos() {
        assertEquals("1111", TarjetaUtil.ultimos4("4111111111111111"));
    }

    @Test
    void elHashEsEstableNoContieneElNumeroYDependeDelPepper() {
        String h1 = TarjetaUtil.hash("4111111111111111", "pepper");
        assertEquals(h1, TarjetaUtil.hash("4111111111111111", "pepper"));
        assertEquals(64, h1.length());
        assertFalse(h1.contains("4111"));
        assertNotEquals(h1, TarjetaUtil.hash("4111111111111111", "otro"));
        assertNotEquals(h1, TarjetaUtil.hash("4111111111111112", "pepper"));
    }

    @Test
    void unaTarjetaVenceAlTerminarSuMes() {
        YearMonth hoy = YearMonth.of(2026, 11);
        assertFalse(TarjetaUtil.vencida("11/26", hoy));
        assertFalse(TarjetaUtil.vencida("01/30", hoy));
        assertTrue(TarjetaUtil.vencida("10/26", hoy));
        assertTrue(TarjetaUtil.vencida("12/25", hoy));
    }
}
