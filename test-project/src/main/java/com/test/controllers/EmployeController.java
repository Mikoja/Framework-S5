package com.test.controllers;

import com.sprint.framework.exception.BindingException;
import framework.annotations.Controller;
import framework.annotations.Post;
import framework.annotations.RequestParam;
import framework.annotations.UrlMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class EmployeController {

    @UrlMapping("/employe/form")
    public String form() {
        return "formulaire.jsp";
    }

    @UrlMapping("/employe/save")
    @Post
    public String save(HttpServletRequest request, @RequestParam("nom") String nom,
                       int age, double salaire) throws BindingException {
        return "Enregistrement : " + nom + ", age=" + age + ", salaire=" + salaire;
    }
}
