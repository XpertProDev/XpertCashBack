package com.xpertcash.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        // Spring injecte automatiquement l’objet CsrfToken
        return token;
    }
}
