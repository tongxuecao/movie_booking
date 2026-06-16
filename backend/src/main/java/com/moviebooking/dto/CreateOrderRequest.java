package com.moviebooking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateOrderRequest {
    @NotNull(message = "场次ID不能为空")
    private Long showtimeId;

    @NotEmpty(message = "座位ID不能为空")
    private List<Long> seatIds;

    @NotBlank(message = "lockToken不能为空")
    private String lockToken;

    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long showtimeId) { this.showtimeId = showtimeId; }

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public String getLockToken() { return lockToken; }
    public void setLockToken(String lockToken) { this.lockToken = lockToken; }
}
