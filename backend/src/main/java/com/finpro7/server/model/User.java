package com.finpro7.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    private String role;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int coins = 0;

    // ini buat nyimpen status kepemilikan item itemnya
    @Column(columnDefinition = "boolean default false")
    private boolean ownZippy = false;

    @Column(columnDefinition = "boolean default false")
    private boolean ownChunky = false;

    @Column(columnDefinition = "boolean default false")
    private boolean ownLong = false;

    @Column(columnDefinition = "boolean default false")
    private boolean ownScoped = false;

    // penanda punya senjata ak atau enggak
    @Column(columnDefinition = "boolean default false")
    private boolean hasAk = false;

    // nyimpen id pistol yang lagi dipake sekarang (0-4)
    @Column(nullable = false, columnDefinition = "int default 0")
    private int equippedPistolId = 0;

    // getter setter standar kyak biasa biar variabelnya bisa diakses
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isHasAk() { return hasAk; }
    public void setHasAk(boolean hasAk) { this.hasAk = hasAk; }

    // buat ngecek atau ngatur kepemilikan pistol zippy
    public boolean isOwnZippy() {
        return ownZippy;
    }

    public void setOwnZippy(boolean ownZippy) {
        this.ownZippy = ownZippy;
    }

    // buat ngecek atau ngatur kepemilikan pistol chunky
    public boolean isOwnChunky() {
        return ownChunky;
    }

    public void setOwnChunky(boolean ownChunky) {
        this.ownChunky = ownChunky;
    }

    // buat ngecek atau ngatur kepemilikan pistol long barrel
    public boolean isOwnLong() {
        return ownLong;
    }

    public void setOwnLong(boolean ownLong) {
        this.ownLong = ownLong;
    }

    // buat ngecek atau ngatur kepemilikan pistol scoped
    public boolean isOwnScoped() {
        return ownScoped;
    }

    public void setOwnScoped(boolean ownScoped) {
        this.ownScoped = ownScoped;
    }

    // bagian getter setter buat sistem equip senjata
    // kodenya: 0=regular, 1=zippy, 2=chunky, 3=long, 4=scoped
    public int getEquippedPistolId() {
        return equippedPistolId;
    }

    public void setEquippedPistolId(int equippedPistolId) {
        this.equippedPistolId = equippedPistolId;
    }
}
