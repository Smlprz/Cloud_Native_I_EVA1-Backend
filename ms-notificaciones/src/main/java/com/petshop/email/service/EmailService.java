package com.petshop.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;
    private final boolean simulado;
    private final String destinoForzado;

    /**
     * simulado (app.mail.simulado=true): el correo solo se escribe en el log y no se abre ninguna
     * conexion SMTP. Sirve para demostrar el flujo sin credenciales de Gmail.
     * destinoForzado (app.mail.destino-forzado): si se define, TODO correo se entrega a esa direccion
     * en vez de al cliente (el email del JWT de Azure puede no tener buzon, p. ej. *.onmicrosoft.com).
     */
    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String remitente,
                        @Value("${app.mail.simulado:false}") boolean simulado,
                        @Value("${app.mail.destino-forzado:}") String destinoForzado) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.simulado = simulado;
        this.destinoForzado = destinoForzado == null ? "" : destinoForzado.trim();
    }

    private String destinatario(String to) {
        return destinoForzado.isEmpty() ? to : destinoForzado;
    }

    /** Correo simple, texto plano. */
    public void enviarCorreo(String to, String subject, String body) {
        String original = to;
        to = destinatario(to);
        if (!to.equals(original)) {
            body = body + "\n\n(Destinatario original: " + original + ")";
        }
        if (simulado) {
            log.info("[CORREO SIMULADO] para={} asunto=\"{}\"\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(remitente);
        mailSender.send(message);
    }

    /** Correo HTML, con adjunto opcional. */
    public void enviarCorreoConAdjunto(String to, String subject, String htmlContent, String rutaArchivo)
            throws MessagingException {
        to = destinatario(to);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(remitente);

        if (rutaArchivo != null && !rutaArchivo.isEmpty()) {
            FileSystemResource file = new FileSystemResource(new File(rutaArchivo));
            helper.addAttachment(file.getFilename(), file);
        }
        mailSender.send(message);
    }
}
