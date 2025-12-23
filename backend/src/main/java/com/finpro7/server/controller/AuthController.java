package com.finpro7.server.controller;

import com.finpro7.server.model.User;
import com.finpro7.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            // balikin error kalau usernamenya udah ada yang pake
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username sudah dipakai!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);

        // bilang oke kalau pendaftaran sukses
        return ResponseEntity.ok("Register Berhasil!");
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser.isPresent()) {
            if (passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
                // kalau password cocok langsung kasih data user nya
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
            // tambahin koin yang ada di db sama koin baru dari game
            user.setCoins(user.getCoins() + req.getCoins());
            userRepository.save(user);
            return ResponseEntity.ok("Koin berhasil masuk database! Total sekarang: " + user.getCoins());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User gak ketemu");
    }

    @PostMapping("/buyItem")
    public ResponseEntity<?> buyItem(@RequestBody Map<String, String> req) {
        // kita pake map string biar fleksibel nerima json apa aja
        String username = req.get("username");
        String itemKey = req.get("itemBought"); // ngambil kunci item misal has_ak atau mod_scope

        System.out.println("Menerima request beli dari: " + username + " item: " + itemKey);

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // cek dulu ini request ganti senjata (equip) atau beli barang
            if (itemKey.startsWith("equip_")) {
                if (itemKey.equals("equip_regular")) user.setEquippedPistolId(0);
                // mastiin user beneran punya senjatanya sebelum di-equip (validasi server)
                else if (itemKey.equals("equip_zippy") && user.isOwnZippy()) user.setEquippedPistolId(1);
                else if (itemKey.equals("equip_chunky") && user.isOwnChunky()) user.setEquippedPistolId(2);
                else if (itemKey.equals("equip_long") && user.isOwnLong()) user.setEquippedPistolId(3);
                else if (itemKey.equals("equip_scoped") && user.isOwnScoped()) user.setEquippedPistolId(4);

                userRepository.save(user);
                System.out.println("Berhasil ganti senjata ke ID: " + user.getEquippedPistolId());
                return ResponseEntity.ok(user); // langsung balikin response biar gak lanjut ke logika bayar
            }

            int harga = 0;
            boolean alreadyOwned = false;

            // tentuin harga dan cek status kepemilikan berdasarkan item yang diminta
            switch (itemKey) {
                case "buy_zippy": harga = 500; if(user.isOwnZippy()) alreadyOwned=true; break;
                case "buy_chunky": harga = 600; if(user.isOwnChunky()) alreadyOwned=true; break;
                case "buy_long": harga = 700; if(user.isOwnLong()) alreadyOwned=true; break;
                case "buy_scoped": harga = 800; if(user.isOwnScoped()) alreadyOwned=true; break;
                // beli ak
                case "buy_ak": harga = 1000; if(user.isHasAk()) alreadyOwned=true; break;
                default:
                    return ResponseEntity.badRequest().body("Item tidak valid!");
            }

            // cek dulu user udah punya barangnya apa belum
            if (alreadyOwned) {
                return ResponseEntity.badRequest().body("User sudah memiliki item ini!");
            }

            // cek duit di kantong cukup gak
            if (user.getCoins() >= harga) {
                // transaksi sukses kurangi koin
                user.setCoins(user.getCoins() - harga);

                // update status kepemilikan di db
                if (itemKey.equals("buy_zippy")) user.setOwnZippy(true);
                else if (itemKey.equals("buy_chunky")) user.setOwnChunky(true);
                else if (itemKey.equals("buy_long")) user.setOwnLong(true);
                else if (itemKey.equals("buy_scoped")) user.setOwnScoped(true);
                else if (itemKey.equals("buy_ak")) user.setHasAk(true);

                // kalau beli pistol, otomatis equip biar user langsung pake
                if (itemKey.equals("buy_zippy")) user.setEquippedPistolId(1);
                else if (itemKey.equals("buy_chunky")) user.setEquippedPistolId(2);
                else if (itemKey.equals("buy_long")) user.setEquippedPistolId(3);
                else if (itemKey.equals("buy_scoped")) user.setEquippedPistolId(4);

                userRepository.save(user);

                System.out.println("Pembelian Sukses! Sisa koin: " + user.getCoins());
                return ResponseEntity.ok(user); // kirim balik data user terbaru
            } else {
                System.out.println("Gagal: Uang tidak cukup. Punya: " + user.getCoins() + ", Butuh: " + harga);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uang tidak cukup!");
            }
        }

        System.out.println("Gagal: User tidak ditemukan di DB");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User gak ketemu");
    }
}
