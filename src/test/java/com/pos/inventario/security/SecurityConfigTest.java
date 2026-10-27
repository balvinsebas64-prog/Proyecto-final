package com.pos.inventario.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import jakarta.servlet.Filter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class SecurityConfigTest {
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, Rutas.class})
    static class Config {
        @Bean
        UserDetailsService usuarios() {
            return new InMemoryUserDetailsManager(User.withUsername("test").password("unused").roles("CAJERO").build());
        }
    }

    @RestController
    static class Rutas {
        @RequestMapping("/**")
        String pagina() { return "ok"; }
    }

    static class Aplicacion implements AutoCloseable {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        final MockMvc mvc;
        Aplicacion() {
            context.setServletContext(new MockServletContext());
            context.register(Config.class);
            context.refresh();
            mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        }
        MockHttpSession sesion(String rol) {
            MockHttpSession session = new MockHttpSession(context.getServletContext());
            var auth = UsernamePasswordAuthenticationToken.authenticated("usuario", "",
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(auth));
            return session;
        }
        @Override public void close() { context.close(); }
    }

    @Test
    void cajeroNoPuedeAbrirNingunaRutaAdministrativa() throws Exception {
        try (Aplicacion app = new Aplicacion()) {
            for (String ruta : List.of("/admin/dashboard", "/productos", "/productos/nuevo", "/productos/editar/1",
                    "/categorias", "/categorias/guardar", "/usuarios", "/usuarios/1", "/reportes", "/ventas/historial")) {
                assertEquals(403, app.mvc.perform(get(ruta).session(app.sesion("CAJERO")))
                        .andReturn().getResponse().getStatus(), ruta);
            }
        }
    }

    @Test
    void adminAccedeASusRutasPeroNoAlPuntoDeVenta() throws Exception {
        try (Aplicacion app = new Aplicacion()) {
            for (String ruta : List.of("/admin/dashboard", "/productos", "/productos/nuevo", "/categorias", "/usuarios", "/reportes", "/ventas/historial")) {
                assertEquals(200, app.mvc.perform(get(ruta).session(app.sesion("ADMIN")))
                        .andReturn().getResponse().getStatus(), ruta);
            }
            for (String ruta : List.of("/ventas", "/ventas/nueva", "/ventas/exito/1", "/cajero/dashboard")) {
                assertEquals(403, app.mvc.perform(get(ruta).session(app.sesion("ADMIN")))
                        .andReturn().getResponse().getStatus(), ruta);
            }
        }
    }

    @Test
    void cajeroAccedeAVentasYAnonimoDebeIniciarSesion() throws Exception {
        try (Aplicacion app = new Aplicacion()) {
            for (String ruta : List.of("/cajero/dashboard", "/ventas", "/ventas/nueva")) {
                assertEquals(200, app.mvc.perform(get(ruta).session(app.sesion("CAJERO")))
                        .andReturn().getResponse().getStatus(), ruta);
                var response = app.mvc.perform(get(ruta)).andReturn().getResponse();
                assertEquals(302, response.getStatus(), ruta);
                assertTrue(response.getRedirectedUrl().endsWith("/login"));
            }
        }
    }

    @Test
    void permisosSeAplicanTambienAPostConCsrfValido() throws Exception {
        try (Aplicacion app = new Aplicacion()) {
            for (String rol : List.of("ADMIN", "CAJERO")) {
                for (String ruta : List.of("/productos/guardar", "/categorias/guardar", "/usuarios/guardar", "/ventas/procesar")) {
                    MockHttpSession session = app.sesion(rol);
                    MockHttpServletRequest request = new MockHttpServletRequest(); request.setSession(session);
                    HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
                    var token = csrf.generateToken(request);
                    csrf.saveToken(token, request, new MockHttpServletResponse());
                    new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler()
                            .handle(request, new MockHttpServletResponse(), () -> token);
                    var tokenFormulario = (org.springframework.security.web.csrf.CsrfToken)
                            request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
                    boolean permitido = ruta.startsWith("/ventas/") ? rol.equals("CAJERO") : rol.equals("ADMIN");
                    assertEquals(permitido ? 200 : 403, app.mvc.perform(post(ruta).session(session)
                            .header(token.getHeaderName(), tokenFormulario.getToken())).andReturn().getResponse().getStatus(), rol + " " + ruta);
                }
            }
        }
    }
}
