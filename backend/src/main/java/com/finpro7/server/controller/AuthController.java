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
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser.isPresent()) {
            if (passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
                // PASSWORD BENAR -> Return 200 OK
                return ResponseEntity.ok(dbUser.get());
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Username atau Password salah!");
    }

    @PostMapping("/saveCoins")
    public ResponseEntity<?> saveCoins(@RequestBody User req) {
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // nambahin koin lama di DB + koin baru dari game
            user.setCoins(user.getCoins() + req.getCoins());
            userRepository.save(user);
            return ResponseEntity.ok("Koin berhasil masuk database! Total sekarang: " + user.getCoins());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User gak ketemu");
    }

    @PostMapping("/buyItem")
    public ResponseEntity<?> buyItem(@RequestBody User req) {
        System.out.println("Menerima request beli dari: " + req.getUsername()); // Debugging

        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 1. Cek apakah user SUDAH PUNYA?
            if (user.isHasAk()) {
                return ResponseEntity.badRequest().body("User sudah memiliki item ini!");
            }

            // 2. Cek UANG (Harga 500 sesuai kode Anda)
            int harga = 500;
            if (user.getCoins() >= harga) {
                // Transaksi Sukses
                user.setCoins(user.getCoins() - harga);
                user.setHasAk(true);
                userRepository.save(user);

                System.out.println("Pembelian Sukses! Sisa koin: " + user.getCoins());
                return ResponseEntity.ok(user); // Kirim balik user yang sudah diupdate
            } else {
                System.out.println("Gagal: Uang tidak cukup. Punya: " + user.getCoins() + ", Butuh: " + harga);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uang tidak cukup!");
            }
        }

        System.out.println("Gagal: User tidak ditemukan di DB");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User gak ketemu");
    }
}
