package com.service.auth;

import com.service.security.JwtService;
import com.service.user.User;
import com.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();
        if (username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Username and password are required."));
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Password must be at most " + MAX_PASSWORD_LENGTH + " characters long."));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Username already taken."));
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(username), username));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password."));
        }
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(username), username));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserResponse(principal.getName()));
    }

    public record AuthRequest(String username, String password) {
    }

    public record AuthResponse(String token, String username) {
    }

    public record UserResponse(String username) {
    }

    public record ErrorResponse(String message) {
    }
}
