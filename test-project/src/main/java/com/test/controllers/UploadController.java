package com.test.controllers;

import framework.annotations.Autowired;
import framework.annotations.Controller;
import framework.annotations.Post;
import framework.annotations.RequestParam;
import framework.annotations.UrlMapping;
import framework.file.FileStorage;
import framework.file.MaFile;

import java.io.IOException;

@Controller
public class UploadController {

    @Autowired
    private FileStorage fileStorage;

    @UrlMapping("/upload/form")
    public String form() {
        return "upload";
    }

    @Post("/upload/save")
    public String save(@RequestParam("fichier") MaFile fichier,
                       @RequestParam(value = "description", required = false) String description) throws IOException {
        if (fichier == null || fichier.isEmpty()) {
            return "Aucun fichier recu";
        }
        String chemin = fileStorage.save(fichier);
        return "Fichier recu : " + fichier.getFileName() + " (." + fichier.getType() + ", "
                + fichier.getData().length + " octets)" + ", enregistre dans " + chemin;
    }
}
