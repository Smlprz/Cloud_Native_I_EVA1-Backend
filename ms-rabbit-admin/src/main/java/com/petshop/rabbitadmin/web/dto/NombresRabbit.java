package com.petshop.rabbitadmin.web.dto;

/**
 * Reglas para nombres de colas, exchanges y routing keys. RabbitMQ admite hasta 255 bytes;
 * aqui se restringe a letras, numeros y . _ : - (sin espacios ni simbolos raros), y se rechaza
 * el prefijo reservado "amq." que el broker no permite declarar.
 */
public final class NombresRabbit {

    public static final int MAX = 255;
    public static final String NOMBRE_REGEX = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._:-]*$";
    public static final String NOMBRE_MENSAJE =
            "solo letras, numeros y . _ : - (debe empezar con letra o numero y no usar el prefijo reservado 'amq.')";

    /** La routing key puede ser vacia o usar comodines de topic (* y #). */
    public static final String ROUTING_KEY_REGEX = "^[A-Za-z0-9._:*#-]*$";
    public static final String ROUTING_KEY_MENSAJE = "solo letras, numeros y . _ : - * #";

    private NombresRabbit() {
    }
}
