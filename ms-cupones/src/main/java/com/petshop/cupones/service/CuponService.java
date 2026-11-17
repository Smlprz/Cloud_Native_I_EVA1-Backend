package com.petshop.cupones.service;

import com.petshop.cupones.domain.Cupon;
import com.petshop.cupones.domain.CuponUso;
import com.petshop.cupones.domain.TipoCupon;
import com.petshop.cupones.repository.CuponRepository;
import com.petshop.cupones.repository.CuponUsoRepository;
import com.petshop.cupones.web.dto.CalculoResponse;
import com.petshop.cupones.web.dto.CuponRequest;
import com.petshop.cupones.web.dto.CuponResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de los cupones. Un cupon se puede usar si: existe y esta activo, no vencio, la compra alcanza el monto minimo,
 * aun quedan usos y el usuario no lo habia usado antes.
 *
 *  - validar(): calcula el descuento SIN consumir el cupon (para mostrarlo antes de comprar).
 *  - aplicar(): lo consume. El uso se descuenta con un UPDATE condicional atomico y la restriccion unica
 *    (cupon, usuario) impide el doble uso, asi que ni las solicitudes simultaneas pueden pasarse de los usos maximos.
 */
@Service
public class CuponService {

    private static final Logger log = LoggerFactory.getLogger(CuponService.class);

    private final CuponRepository cupones;
    private final CuponUsoRepository usos;
    private final Clock reloj;

    @Autowired
    public CuponService(CuponRepository cupones, CuponUsoRepository usos) {
        this(cupones, usos, Clock.systemDefaultZone());
    }

    CuponService(CuponRepository cupones, CuponUsoRepository usos, Clock reloj) {
        this.cupones = cupones;
        this.usos = usos;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ administracion

    @Transactional
    public CuponResponse crear(CuponRequest request) {
        String codigo = normalizar(request.codigo());
        if (request.tipo() == TipoCupon.PORCENTAJE && request.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw CuponException.invalido("Un cupon de porcentaje no puede superar el 100 %");
        }
        if (request.venceEn() != null && request.venceEn().isBefore(LocalDateTime.now(reloj))) {
            throw CuponException.invalido("La fecha de vencimiento ya paso");
        }
        if (cupones.existsByCodigo(codigo)) {
            throw CuponException.codigoDuplicado(codigo);
        }
        try {
            Cupon cupon = cupones.saveAndFlush(new Cupon(codigo, request.tipo(), request.valor(),
                    request.montoMinimo(), request.venceEn(), request.usosMaximos()));
            log.info("Cupon creado: {} ({} {})", codigo, request.tipo(), request.valor());
            return CuponResponse.from(cupon);
        } catch (DataIntegrityViolationException e) {
            throw CuponException.codigoDuplicado(codigo); // dos administradores creandolo a la vez
        }
    }

    @Transactional(readOnly = true)
    public List<CuponResponse> listar() {
        return cupones.findAllByOrderByCreadoEnDesc().stream().map(CuponResponse::from).toList();
    }

    @Transactional
    public CuponResponse cambiarActivo(Long id, boolean activo) {
        Cupon cupon = cupones.findById(id).orElseThrow(CuponException::noExiste);
        cupon.setActivo(activo);
        log.info("Cupon {} {}", cupon.getCodigo(), activo ? "activado" : "desactivado");
        return CuponResponse.from(cupon);
    }

    // ---------------------------------------------------------------------- uso

    @Transactional(readOnly = true)
    public CalculoResponse validar(String usuarioId, String codigo, BigDecimal monto) {
        Cupon cupon = verificar(usuarioId, codigo, monto);
        return calculo(cupon, monto);
    }

    @Transactional
    public CalculoResponse aplicar(String usuarioId, String codigo, BigDecimal monto) {
        Cupon cupon = verificar(usuarioId, codigo, monto);
        if (cupones.consumirUso(cupon.getId()) == 0) {
            throw CuponException.agotado();
        }
        CalculoResponse resultado = calculo(cupon, monto);
        try {
            usos.saveAndFlush(new CuponUso(cupon.getId(), usuarioId, monto, resultado.descuento()));
        } catch (DataIntegrityViolationException e) {
            throw CuponException.yaUsado(); // dos solicitudes del mismo usuario a la vez: la transaccion se revierte
        }
        log.info("Cupon {} aplicado por {}: {} de descuento sobre {}", cupon.getCodigo(), usuarioId,
                resultado.descuento(), monto);
        return resultado;
    }

    private Cupon verificar(String usuarioId, String codigo, BigDecimal monto) {
        Cupon cupon = cupones.findByCodigo(normalizar(codigo)).filter(Cupon::isActivo)
                .orElseThrow(CuponException::noExiste);
        if (cupon.estaVencido(LocalDateTime.now(reloj))) {
            throw CuponException.vencido();
        }
        if (monto.compareTo(cupon.getMontoMinimo()) < 0) {
            throw CuponException.montoMinimo(cupon.getMontoMinimo());
        }
        if (cupon.getUsosMaximos() != null && cupon.getUsosActuales() >= cupon.getUsosMaximos()) {
            throw CuponException.agotado();
        }
        if (usos.existsByCuponIdAndUsuarioId(cupon.getId(), usuarioId)) {
            throw CuponException.yaUsado();
        }
        return cupon;
    }

    private static CalculoResponse calculo(Cupon cupon, BigDecimal monto) {
        BigDecimal descuento = cupon.descuentoSobre(monto);
        return new CalculoResponse(cupon.getCodigo(), monto, descuento, monto.subtract(descuento));
    }

    private static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
