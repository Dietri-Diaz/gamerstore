package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.Instant;

// Entidad RefreshToken: token de renovación de sesión asociado a un Usuario, mapea a la tabla "refresh_token"
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Valor único del refresh token
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    // Relación N:1 con el Usuario dueño del token
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado = false;

    public RefreshToken() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public boolean isRevocado() { return revocado; }
    public void setRevocado(boolean revocado) { this.revocado = revocado; }

    // Getter derivado: true si el token no está revocado y aún no expiró
    public boolean estaVigente() {
        return !revocado && expiraEn.isAfter(Instant.now());
    }
}
