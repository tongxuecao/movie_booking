package com.moviebooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class BatchShowtimeRequest {
    private Long movieId;
    private Long hallId;
    private LocalDate showDate;
    private List<LocalTime> times;
    private BigDecimal price;

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getHallId() { return hallId; }
    public void setHallId(Long hallId) { this.hallId = hallId; }

    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public List<LocalTime> getTimes() { return times; }
    public void setTimes(List<LocalTime> times) { this.times = times; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
