package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Entidad Comprobante: boleta de venta emitida al aprobarse un pago, mapea a la tabla "comprobante"
@Entity
@Table(name = "comprobante")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Por ahora solo emitimos "BOLETA" (persona natural / DNI, sin RUC de cliente)
    @Column(nullable = false, length = 20)
    private String tipo = "BOLETA";

    @Column(nullable = false, length = 6)
    private String serie;

    // Correlativo dentro de la serie
    @Column(nullable = false)
    private int numero;

    // Relación N:1 con el Pedido que originó esta boleta (obligatoria)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Datos del cliente "fotografiados" al momento de emitir (no cambian si el cliente se edita después)
    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "cliente_dni", length = 8)
    private String clienteDni;

    @Column(name = "cliente_direccion", length = 200)
    private String clienteDireccion;

    // Operación gravada (sin IGV)
    @Column(nullable = false)
    private double subtotal;

    @Column(nullable = false)
    private double igv;

    @Column(nullable = false)
    private double total;

    @Column(nullable = false, length = 5)
    private String moneda = "PEN";

    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @Column(name = "referencia_pago", length = 60)
    private String referenciaPago;

    @Column(nullable = false, length = 20)
    private String estado = "EMITIDO";

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    public Comprobante() {}

    // Se ejecuta antes de insertar: fija la fecha de emisión si no vino seteada
    @PrePersist
    protected void onCreate() {
        if (fechaEmision == null) fechaEmision = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteDni() { return clienteDni; }
    public void setClienteDni(String clienteDni) { this.clienteDni = clienteDni; }
    public String getClienteDireccion() { return clienteDireccion; }
    public void setClienteDireccion(String clienteDireccion) { this.clienteDireccion = clienteDireccion; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getIgv() { return igv; }
    public void setIgv(double igv) { this.igv = igv; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    // Getter derivado: código de la boleta con formato SERIE-00000001 (ej. B001-00000001)
    public String getCodigo() { return serie + "-" + String.format("%08d", numero); }
}
