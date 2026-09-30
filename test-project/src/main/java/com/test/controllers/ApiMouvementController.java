package com.test.controllers;

import com.test.entity.Mouvement;
import com.test.repository.MouvementRepository;
import framework.annotations.Autowired;
import framework.annotations.Controller;
import framework.annotations.Get;
import framework.annotations.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur d'API REST : chaque méthode renvoie directement la donnée à
 * sérialiser, aucune vue n'est dispatchée. Le mapping d'URL reste identique à
 * celui d'un contrôleur classique.
 */
@Controller("/api/mouvements")
public class ApiMouvementController {

    @Autowired
    private MouvementRepository repository;

    /** &rarr; {@code [{"id":1,"type":"DEBIT",...}]} */
    @Get
    @Json
    public List<Mouvement> liste() {
        return repository.findAll();
    }

    /** &rarr; {@code {"nombre":3,"montantTotal":150.5}} */
    @Get("/statistiques")
    @Json
    public Map<String, Object> statistiques() {
        List<Mouvement> mouvements = repository.findAll();

        Map<String, Object> statistiques = new LinkedHashMap<>();
        statistiques.put("nombre", mouvements.size());
        statistiques.put("montantTotal", mouvements.stream().mapToDouble(Mouvement::getMontant).sum());
        return statistiques;
    }

    /** &rarr; {@code "L'API REST du framework fonctionne"} */
    @Get("/message")
    @Json
    public String message() {
        return "L'API REST du framework fonctionne";
    }

    /** &rarr; {@code {"status":"ok","service":"framework-s6"}} */
    @Get("/raw")
    @Json(raw = true)
    public String dejaJson() {
        return "{\"status\":\"ok\",\"service\":\"framework-s6\"}";
    }

    /** &rarr; {@code {"message":"Sprint 6 — API REST","version":6}} */
    @Get("/infos")
    @Json
    public Infos infos() {
        return new Infos("Sprint 6 — API REST", 6);
    }

    public static class Infos {

        private final String message;
        private final int version;

        public Infos(String message, int version) {
            this.message = message;
            this.version = version;
        }

        public String getMessage() {
            return message;
        }

        public int getVersion() {
            return version;
        }
    }
}
