package com.moviebooking.dto;

public class UpdateProfileRequest {
    private String phone;
    private String avatar;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
