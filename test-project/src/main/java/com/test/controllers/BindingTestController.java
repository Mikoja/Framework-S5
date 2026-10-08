package com.test.controllers;

import com.sprint.framework.exception.BindingException;
import framework.annotations.Controller;
import framework.annotations.Post;
import framework.annotations.RequestParam;
import framework.annotations.UrlMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

@Controller
public class BindingTestController {

    @UrlMapping("/test/binding")
    @Post
    public String binding(@RequestParam("nom") String nom, int age, double salaire) {
        return "ok";
    }

    @UrlMapping("/test/req")
    @Post
    public String req(HttpServletRequest request, String nom) {
        return "ok";
    }

    @UrlMapping("/test/default")
    @Post
    public String def(@RequestParam(value = "nom", required = false) String nom,
                      @RequestParam(value = "age", required = false) Integer age,
                      @RequestParam(value = "actif", required = false) boolean actif) {
        return "ok";
    }

    @UrlMapping("/test/invalid")
    @Post
    public String invalid(int age) throws BindingException {
        return "ok";
    }

    @UrlMapping("/test/complex")
    @Post
    public String complex(Date date) throws BindingException {
        return "ok";
    }
}
