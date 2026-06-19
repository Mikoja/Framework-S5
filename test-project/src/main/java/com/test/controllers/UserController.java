package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller("/users")
public class UserController {

    @Get
    public String list() {
        return "users";
    }
}