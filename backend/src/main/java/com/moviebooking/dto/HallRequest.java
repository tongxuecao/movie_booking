package com.moviebooking.dto;

public class HallRequest {
    private Long cinemaId;
    private String name;
    private Integer seatRows;
    private Integer seatCols;
    private String hallType;

    public Long getCinemaId() { return cinemaId; }
    public void setCinemaId(Long cinemaId) { this.cinemaId = cinemaId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSeatRows() { return seatRows; }
    public void setSeatRows(Integer seatRows) { this.seatRows = seatRows; }

    public Integer getSeatCols() { return seatCols; }
    public void setSeatCols(Integer seatCols) { this.seatCols = seatCols; }

    public String getHallType() { return hallType; }
    public void setHallType(String hallType) { this.hallType = hallType; }
}
