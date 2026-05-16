package com.lexoft.rag.controller;

import com.lexoft.rag.model.WelcomeResponse;
import com.lexoft.rag.service.WelcomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    private final WelcomeService welcomeService;

    public WelcomeController(WelcomeService welcomeService) {
        this.welcomeService = welcomeService;
    }

    @GetMapping(path = "/welcome", produces = "application/json")
    public WelcomeResponse welcome(
            @RequestParam String username,
            @RequestParam String role) {
        return new WelcomeResponse(welcomeService.welcome(username, role));
    }
}
