package com.gamerstore.app.model;

// Roles posibles de un usuario del sistema.
// Solo queda ADMIN: la tienda pública no necesita login (se compra como invitado), así que
// todo usuario que entra al sistema entra al panel. El rol USUARIO existía pero igual tenía
// acceso a todo el panel, o sea que "limitaba" en el papel y no en la práctica: era una falla
// de seguridad silenciosa. Se eliminó para que el permiso real y el rol digan lo mismo.
public enum Rol {
    ADMIN
}
