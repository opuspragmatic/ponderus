package io.pragmatic.ponderus.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.pragmatic.ponderus.domain.User;


@RestController
public class JwtController {
    
    @GetMapping("/api/me")
    public String me(@AuthenticationPrincipal User user) {
        return user.getEmail(); // = le firebase_uid
    }
}
