package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;
import framework.annotations.Post;

@Controller("/hello")
public class HelloController {

    @Get
    public String index() {
        return "hello";
    }

    @Get("/world")
    public String world() {
        return "world";
    }

    @Get("/submit")
    public String submitForm() {
        return "submit-form";
    }

    @Post("/submit")
    public String submit() {
        return "submitted";
    }
}
