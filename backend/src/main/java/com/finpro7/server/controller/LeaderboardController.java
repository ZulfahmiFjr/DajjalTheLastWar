package com.finpro7.server.controller;

import com.finpro7.server.model.User;
import com.finpro7.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api") // Kita pake path /api biar rapi
public class LeaderboardController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        // Ambil data user yang udah diurutin sama repository tadi
        List<User> topUsers = userRepository.findTop100ByOrderByCoinsDesc();

        // Siapin list kosong buat nampung data yang udah "bersih"
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        // Kita looping, pindahin data penting aja ke format baru
        for (User u : topUsers) {
            leaderboard.add(new LeaderboardEntry(
                u.getUsername(),
                u.getCoins(),
                u.getEquippedPistolId()
            ));
        }

        // Kirim balik ke game!
        return ResponseEntity.ok(leaderboard);
    }

    // Ini kelas kecil (DTO) buat ngebungkus data biar rapi & aman
    // Sengaja dibikin di dalem sini aja biar gak nambah file kebanyakan
    static class LeaderboardEntry {
        public String username;
        public int coins;
        public int weaponId; // biar ketauan dia lagi pake senjata apa

        public LeaderboardEntry(String username, int coins, int weaponId) {
            this.username = username;
            this.coins = coins;
            this.weaponId = weaponId;
        }
    }
}
