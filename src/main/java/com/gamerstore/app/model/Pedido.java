package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entidad Pedido: representa una venta/orden de un cliente, mapea a la tabla "pedido"
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación N:1 con Cliente (obligatoria)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private double total;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    // Cómo recibe el comprador su pedido: "RECOJO_TIENDA" o "DELIVERY".
    // OJO: nullable a propósito (igual que las 3 columnas de abajo). La BD ya tiene pedidos
    // viejos y con ddl-auto=update Hibernate no puede agregar una columna NOT NULL a una
    // tabla con filas. El valor por defecto se pone en @PrePersist, no en la columna.
    @Column(name = "tipo_entrega", length = 20)
    private String tipoEntrega;

    // Dirección a la que se envía. Solo se llena si tipoEntrega == DELIVERY.
    @Column(name = "direccion_envio", length = 200)
    private String direccionEnvio;

    // Referencia opcional para ubicar la dirección ("portón azul", "al costado de la bodega").
    @Column(name = "referencia_envio", length = 150)
    private String referenciaEnvio;

    // Por qué se anuló la venta. Se llena solo cuando el pedido pasa a ANULADO (ver AnulacionService).
    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    // Relación 1:N con las líneas del pedido; se guardan/eliminan en cascada junto con el pedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> items = new ArrayList<>();

    public Pedido() {}

    // Se ejecuta antes de insertar: fija fecha, estado y tipo de entrega por defecto si faltan
    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
        // Por defecto recojo en tienda: es la opción que no necesita dirección, así que
        // un pedido sin datos de entrega (p. ej. una venta cargada desde el panel) queda válido.
        if (tipoEntrega == null) tipoEntrega = "RECOJO_TIENDA";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public List<PedidoItem> getItems() { return items; }
    public void setItems(List<PedidoItem> items) { this.items = items; }
    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String tipoEntrega) { this.tipoEntrega = tipoEntrega; }
    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }
    public String getReferenciaEnvio() { return referenciaEnvio; }
    public void setReferenciaEnvio(String referenciaEnvio) { this.referenciaEnvio = referenciaEnvio; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    // Getter derivado: código de pedido con formato PED-0001
    public String getCodigo() { return String.format("PED-%04d", id == null ? 0 : id); }
    // Getter derivado: suma las cantidades de todas las líneas del pedido
    public int getCantidadTotal() { return items.stream().mapToInt(PedidoItem::getCantidad).sum(); }
}
