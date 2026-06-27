package com.moviebooking.dto;

import java.math.BigDecimal;

public class UserUpdateRequest {
    private String username;
    private String phone;
    private String role;
    private BigDecimal walletBalance;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
