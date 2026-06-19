package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;

@Controller("/test")
public class TestController {

    @Get
    public String list() {
        return "test";
    }
}