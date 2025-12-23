package com.finpro7.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String cekStatus() {
        return "Backend Aman! Server jalan.";
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
