package com.jobportal.controllers;

import com.jobportal.models.Role;
import com.jobportal.models.User;
import com.jobportal.repositories.UserRepository;
import com.jobportal.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // --- DTOs as inner records ---

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull Role role
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, Long id, String name, String email, String role) {}

    // --- Endpoints ---

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Reject ADMIN registration from public endpoint
        if (request.role() == Role.ADMIN) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot register as ADMIN via public registration"));
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getId(), user.getName(),
                        user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole().name()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
    }
}
