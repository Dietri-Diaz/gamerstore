package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Entidad Pago: registra el cobro (Yape o tarjeta) asociado a un pedido, mapea a la tabla "pago"
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación N:1 con Pedido (obligatoria)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // "YAPE" o "TARJETA"
    @Column(nullable = false, length = 20)
    private String metodo;

    @Column(nullable = false)
    private double monto;

    // "APROBADO" o "RECHAZADO"
    @Column(nullable = false, length = 20)
    private String estado;

    // N° de operación (Yape) o código de autorización (tarjeta)
    @Column(nullable = false, length = 60)
    private String referencia;

    @Column(name = "tarjeta_ult4", length = 4)
    private String tarjetaUlt4;

    @Column(length = 100)
    private String titular;

    // Ruta de la captura/voucher (Yape)
    @Column(length = 300)
    private String voucher;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Pago() {}

    // Se ejecuta antes de insertar: fija la fecha actual si no vino seteada
    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getTarjetaUlt4() { return tarjetaUlt4; }
    public void setTarjetaUlt4(String tarjetaUlt4) { this.tarjetaUlt4 = tarjetaUlt4; }
    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }
    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    // Getter derivado: código de pago con formato PAG-0001
    public String getCodigo() { return String.format("PAG-%04d", id == null ? 0 : id); }
}
