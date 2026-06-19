package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller
public class HomeController {

    @Get
    public String index() {
        return "home";
    }
}
