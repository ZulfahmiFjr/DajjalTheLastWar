package com.finpro7.server.controller;

import com.finpro7.server.model.User;
import com.finpro7.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            // Return 400 Bad Request kalau username ada
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username sudah dipakai!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);

        // Return 200 OK
        return ResponseEntity.ok("Register Berhasil!");
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        Optional<User> dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser.isPresent()) {
            if (passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
                // PASSWORD BENAR -> Return 200 OK
                return ResponseEntity.ok("Login Sukses! Role kamu: " + dbUser.get().getRole());
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username atau Password salah!");
    }
}
