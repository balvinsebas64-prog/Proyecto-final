package com.pos.inventario.controller;

import com.pos.inventario.entity.Usuario;
import com.pos.inventario.repository.UsuarioRepository;
import com.pos.inventario.web.UsuarioFormulario;
import jakarta.validation.Validation;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioControllerTest {
    static class Registro {
        Usuario guardado;
        boolean duplicado;
        Usuario objetivo;
        Usuario eliminado;
        final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        final UsuarioController controller;
        Registro() {
            UsuarioRepository repo = (UsuarioRepository) Proxy.newProxyInstance(
                    UsuarioRepository.class.getClassLoader(), new Class<?>[]{UsuarioRepository.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("existsByEmailIgnoreCase")) return duplicado;
                        if (method.getName().equals("save")) { guardado = (Usuario) args[0]; return guardado; }
                        if (method.getName().equals("findById")) return java.util.Optional.ofNullable(objetivo);
                        if (method.getName().equals("delete")) { eliminado = (Usuario) args[0]; return null; }
                        throw new AssertionError(method.getName());
                    });
            controller = new UsuarioController(repo, encoder);
        }
    }

    UsuarioFormulario formulario() {
        UsuarioFormulario form = new UsuarioFormulario();
        form.setNombre(" Ana Pérez "); form.setEmail(" ANA@POS.TEST ");
        form.setPassword("Clave-prueba-123"); form.setRol("CAJERO");
        return form;
    }

    @Test
    void impideEliminarseInclusoConCorreoEnDistintaCapitalizacion() {
        Registro registro = new Registro(); registro.objetivo = new Usuario();
        registro.objetivo.setEmail("ADMIN@POS.TEST");
        var flash = new RedirectAttributesModelMap();
        assertEquals("redirect:/usuarios", registro.controller.eliminar(1L, () -> "admin@pos.test", flash));
        assertNull(registro.eliminado);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void eliminaOtroUsuarioYRedirigeConConfirmacion() {
        Registro registro = new Registro(); registro.objetivo = new Usuario();
        registro.objetivo.setEmail("cajero@pos.test");
        assertEquals("redirect:/usuarios?eliminado=true", registro.controller.eliminar(2L, () -> "admin@pos.test", new RedirectAttributesModelMap()));
        assertSame(registro.objetivo, registro.eliminado);
    }

    BeanPropertyBindingResult validar(UsuarioFormulario form) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "formulario");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            new SpringValidatorAdapter(factory.getValidator()).validate(form, errors);
        }
        return errors;
    }

    @Test
    void guardaHashVerificableYNormalizaCorreo() {
        Registro registro = new Registro(); UsuarioFormulario form = formulario();
        String original = form.getPassword();
        assertEquals("redirect:/usuarios?exito=true", registro.controller.guardar(form, validar(form), new RedirectAttributesModelMap()));
        assertEquals("Ana Pérez", registro.guardado.getNombre());
        assertEquals("ana@pos.test", registro.guardado.getEmail());
        assertNotEquals(original, registro.guardado.getPassword());
        assertTrue(registro.encoder.matches(original, registro.guardado.getPassword()));
        assertEquals("CAJERO", registro.guardado.getRol());
        assertNull(registro.guardado.getId());
        assertNull(form.getPassword());
    }

    @Test
    void rechazaCorreoDuplicadoSinGuardar() {
        Registro registro = new Registro(); registro.duplicado = true;
        UsuarioFormulario form = formulario(); var errors = validar(form);
        assertEquals("usuarios/formulario", registro.controller.guardar(form, errors, new RedirectAttributesModelMap()));
        assertTrue(errors.hasFieldErrors("email"));
        assertNull(registro.guardado); assertNull(form.getPassword());
    }

    @Test
    void rechazaRolInventadoCorreoInvalidoYNombreVacio() {
        Registro registro = new Registro(); UsuarioFormulario form = formulario();
        form.setRol("SUPERADMIN"); form.setEmail("incorrecto"); form.setNombre("   ");
        var errors = validar(form);
        assertTrue(errors.hasFieldErrors("rol"));
        assertTrue(errors.hasFieldErrors("email"));
        assertTrue(errors.hasFieldErrors("nombre"));
        assertEquals("usuarios/formulario", registro.controller.guardar(form, errors, new RedirectAttributesModelMap()));
        assertNull(registro.guardado);
    }

    @Test
    void rechazaPasswordQueSuperaLimiteDeBytesDeBcrypt() {
        Registro registro = new Registro(); UsuarioFormulario form = formulario();
        form.setPassword("á".repeat(40)); var errors = validar(form);
        assertEquals("usuarios/formulario", registro.controller.guardar(form, errors, new RedirectAttributesModelMap()));
        assertTrue(errors.hasFieldErrors("password"));
        assertNull(registro.guardado); assertNull(form.getPassword());
    }
}
