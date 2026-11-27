package com.petshop.analytics.ws;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integraciones del API Gateway WebSocket (pedidos360-ws):
 *   $connect    -> POST /ws/gateway/conectar?token=...  (cabecera connectionId). 200 acepta; 401/403 rechaza.
 *   $disconnect -> POST /ws/gateway/desconectar         (cabecera connectionId).
 * El token de Azure se valida igual que en el WebSocket directo: firma, issuer, audience y rol ADMIN.
 */
@RestController
@RequestMapping("/ws/gateway")
public class ApiGatewayWsController {

    private final JwtHandshakeInterceptor validador;
    private final ApiGatewayWsDifusor difusor;

    public ApiGatewayWsController(JwtHandshakeInterceptor validador, ApiGatewayWsDifusor difusor) {
        this.validador = validador;
        this.difusor = difusor;
    }

    @PostMapping("/conectar")
    public ResponseEntity<String> conectar(@RequestHeader("connectionId") String connectionId,
                                           @RequestParam(value = "token", required = false) String token) {
        JwtHandshakeInterceptor.Rechazo rechazo = validador.validar(token);
        if (rechazo != null) {
            return ResponseEntity.status(rechazo.status()).body(rechazo.motivo());
        }
        difusor.conectar(connectionId);
        return ResponseEntity.ok("conectado");
    }

    @PostMapping("/desconectar")
    public ResponseEntity<Void> desconectar(@RequestHeader("connectionId") String connectionId) {
        difusor.desconectar(connectionId);
        return ResponseEntity.ok().build();
    }
}
