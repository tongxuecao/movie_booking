package com.moviebooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "showtimes")
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "hall_id", nullable = false)
    private Long hallId;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "show_time", nullable = false)
    private LocalTime showTime;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Column(length = 20)
    private String status = "normal";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }

    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public LocalTime getShowTime() { return showTime; }
    public void setShowTime(LocalTime showTime) { this.showTime = showTime; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isExpired() {
        return LocalDateTime.of(showDate, showTime).isBefore(LocalDateTime.now());
    }
}
