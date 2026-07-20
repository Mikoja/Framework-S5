package com.test.controllers;

import com.test.entity.Mouvement;
import com.test.repository.MouvementRepository;
import framework.annotations.Controller;
import framework.annotations.Get;
import framework.model.Model;

import java.util.List;

@Controller("/mouvements")
public class MouvementController {

    private final MouvementRepository repository = new MouvementRepository();

    @Get
    public String liste(Model model) {
        List<Mouvement> mouvements = repository.findAll();
        model.setAttribute("mouvements", mouvements);
        model.setAttribute("titre", "Liste des mouvements");
        return "liste-mouvements";
    }
}
