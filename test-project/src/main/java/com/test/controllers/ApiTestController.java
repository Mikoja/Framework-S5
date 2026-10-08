package com.test.controllers;

import framework.annotations.Controller;
import framework.annotations.Get;
import framework.annotations.Json;

@Controller("/api")
public class ApiTestController {

    @Get("/hello")
    @Json
    public String hello() {
        return "Hello API";
    }
}
