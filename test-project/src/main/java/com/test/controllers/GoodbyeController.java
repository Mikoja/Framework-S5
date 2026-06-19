package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller("/goodbye")
public class GoodbyeController {

    @Get
    public String index() {
        return "goodbye";
    }
}
