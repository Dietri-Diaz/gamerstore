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

    // Relación 1:N con las líneas del pedido; se guardan/eliminan en cascada junto con el pedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> items = new ArrayList<>();

    public Pedido() {}

    // Se ejecuta antes de insertar: fija fecha actual y estado por defecto si faltan
    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = LocalDateTime.now();
        if (estado == null) estado = "PENDIENTE";
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

    // Getter derivado: código de pedido con formato PED-0001
    public String getCodigo() { return String.format("PED-%04d", id == null ? 0 : id); }
    // Getter derivado: suma las cantidades de todas las líneas del pedido
    public int getCantidadTotal() { return items.stream().mapToInt(PedidoItem::getCantidad).sum(); }
}
