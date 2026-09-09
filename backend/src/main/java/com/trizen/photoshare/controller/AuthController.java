package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.*;
import com.trizen.photoshare.security.SecurityUtils;
import com.trizen.photoshare.security.UserPrincipal;
import com.trizen.photoshare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        UserPrincipal principal = securityUtils.getCurrentUser();
        return new UserResponse(
                principal.getId(),
                principal.getUser().getName(),
                principal.getUser().getEmail(),
                principal.getUser().getRole()
        );
    }
}
