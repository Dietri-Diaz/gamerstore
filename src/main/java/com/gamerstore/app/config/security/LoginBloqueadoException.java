package com.gamerstore.app.config.security;

// Se lanza cuando un usuario supero el maximo de intentos fallidos de login y
// todavia esta dentro de la ventana de bloqueo temporal.
public class LoginBloqueadoException extends RuntimeException {
    private final int segundosRestantes;
    public LoginBloqueadoException(int segundosRestantes) {
        super("Demasiados intentos fallidos. Espera " + segundosRestantes + " segundos para volver a intentar.");
        this.segundosRestantes = segundosRestantes;
    }
    public int getSegundosRestantes() { return segundosRestantes; }
}
