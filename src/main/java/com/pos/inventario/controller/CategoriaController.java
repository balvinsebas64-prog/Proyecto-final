package com.pos.inventario.controller;

import com.pos.inventario.entity.Categoria;
import com.pos.inventario.repository.CategoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaRepository.findAll(Sort.by("nombre")));
        return "categorias/lista";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(defaultValue = "") String nombre,
                          Model model, RedirectAttributes redirectAttributes) {
        String nombreLimpio = nombre.strip();
        if (nombreLimpio.isBlank() || nombreLimpio.length() > 100) {
            model.addAttribute("nombre", nombre);
            model.addAttribute("error", "Ingresa un nombre de entre 1 y 100 caracteres.");
            return listar(model);
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombreLimpio);
        categoriaRepository.save(categoria);
        redirectAttributes.addFlashAttribute("exito", "Categoría creada correctamente.");
        return "redirect:/categorias";
    }
}
