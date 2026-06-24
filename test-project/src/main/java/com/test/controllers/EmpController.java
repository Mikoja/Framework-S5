package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.UrlMapping;

@Controller
public class EmpController {

    @UrlMapping("/emp/list")
    public String liste() {
        return "liste des employés";
    }

    @UrlMapping("/emp/new")
    public String creation() {
        return "création d'un employé";
    }
}
