package com.pos.inventario.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Solo recibe los campos de creación; nunca permite enviar un ID desde el formulario.
public class UsuarioFormulario {
    @NotBlank(message = "Ingresa el nombre.")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Ingresa el correo electrónico.")
    @Email(message = "Ingresa un correo electrónico válido.")
    @Size(max = 100, message = "El correo admite hasta 100 caracteres.")
    private String email;

    @NotBlank(message = "Ingresa una contraseña.")
    @Size(max = 72, message = "La contraseña admite hasta 72 caracteres y 72 bytes UTF-8.")
    private String password;

    @NotBlank(message = "Selecciona un rol.")
    @Pattern(regexp = "ADMIN|CAJERO", message = "El rol debe ser ADMIN o CAJERO.")
    private String rol = "CAJERO";

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre == null ? null : nombre.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.strip(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
