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
    @Column(columnDefinition = "boolean default false")
    private boolean hasAk = false;

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
}
