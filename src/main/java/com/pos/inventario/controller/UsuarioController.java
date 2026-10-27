package com.pos.inventario.controller;

import com.pos.inventario.entity.Usuario;
import com.pos.inventario.repository.UsuarioRepository;
import com.pos.inventario.web.UsuarioFormulario;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll(Sort.by("nombre")));
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("formulario", new UsuarioFormulario());
        return "usuarios/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, Principal principal, RedirectAttributes flash) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            flash.addFlashAttribute("error", "El usuario ya no existe.");
            return "redirect:/usuarios";
        }
        if (usuario.getEmail().equalsIgnoreCase(principal.getName())) {
            flash.addFlashAttribute("error", "No puedes eliminar tu propio usuario.");
            return "redirect:/usuarios";
        }
        try {
            usuarioRepository.delete(usuario);
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "No se puede eliminar este usuario porque tiene registros asociados, como ventas.");
            return "redirect:/usuarios";
        }
        flash.addFlashAttribute("exito", "Usuario eliminado correctamente.");
        return "redirect:/usuarios?eliminado=true";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("formulario") UsuarioFormulario formulario,
                          BindingResult errores, RedirectAttributes flash) {
        if (formulario.getPassword() != null
                && formulario.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            errores.rejectValue("password", "password.largo", "La contraseña no puede superar 72 bytes UTF-8.");
        }
        String email = formulario.getEmail() == null ? "" : formulario.getEmail().strip().toLowerCase(Locale.ROOT);
        if (!errores.hasFieldErrors("email") && usuarioRepository.existsByEmailIgnoreCase(email)) {
            errores.rejectValue("email", "email.duplicado", "Ya existe un usuario con este correo.");
        }
        if (errores.hasErrors()) {
            formulario.setPassword(null);
            return "usuarios/formulario";
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(formulario.getNombre().strip());
        usuario.setEmail(email);
        usuario.setRol(formulario.getRol());
        usuario.setPassword(passwordEncoder.encode(formulario.getPassword()));
        formulario.setPassword(null);
        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            errores.reject("usuario.conflicto", "No se pudo guardar. Verifica que el correo no esté registrado.");
            return "usuarios/formulario";
        }
        flash.addFlashAttribute("exito", "Usuario creado correctamente.");
        return "redirect:/usuarios?exito=true";
    }
}
