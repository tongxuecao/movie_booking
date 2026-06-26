package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.dto.CinemaRequest;
import com.moviebooking.dto.HallRequest;
import com.moviebooking.entity.Cinema;
import com.moviebooking.entity.Hall;
import com.moviebooking.entity.Seat;
import com.moviebooking.entity.enums.HallType;
import com.moviebooking.entity.enums.SeatStatus;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.repository.CinemaRepository;
import com.moviebooking.repository.HallRepository;
import com.moviebooking.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;

    @Autowired
    public CinemaService(CinemaRepository cinemaRepository, HallRepository hallRepository, SeatRepository seatRepository) {
        this.cinemaRepository = cinemaRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
    }

    public PageResult<Map<String, Object>> getCinemaList(int page, int size) {
        Page<Cinema> cinemaPage = cinemaRepository.findAll(PageRequest.of(page - 1, size));
        var list = cinemaPage.getContent().stream().map(this::toCinemaMap).toList();
        return new PageResult<>(list, cinemaPage.getTotalElements(), page, size);
    }

    // --- Admin methods ---

    public Map<String, Object> createCinema(CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.getName());
        cinema.setAddress(request.getAddress());
        cinema.setPhone(request.getPhone());
        cinema.setBusinessHours(request.getBusinessHours());
        cinema = cinemaRepository.save(cinema);
        return Map.of("id", cinema.getId());
    }

    @Transactional
    public Map<String, Object> createHall(HallRequest request) {
        Hall hall = new Hall();
        hall.setCinemaId(request.getCinemaId());
        hall.setName(request.getName());
        hall.setSeatRows(request.getSeatRows());
        hall.setSeatCols(request.getSeatCols());
        hall.setHallType(HallType.valueOf(request.getHallType()));
        hall = hallRepository.save(hall);

        // 自动生成座位（全部为 normal）
        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= request.getSeatRows(); row++) {
            for (int col = 1; col <= request.getSeatCols(); col++) {
                Seat seat = new Seat();
                seat.setHallId(hall.getId());
                seat.setRowNum(row);
                seat.setColNum(col);
                seat.setSeatType(SeatType.normal);
                seat.setStatus(SeatStatus.active);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);

        return Map.of("id", hall.getId(), "seatCount", seats.size());
    }

    private Map<String, Object> toCinemaMap(Cinema c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("address", c.getAddress());
        map.put("phone", c.getPhone());
        return map;
    }

    public List<Map<String, Object>> getCinemaHalls(Long cinemaId) {
        List<Hall> halls = hallRepository.findByCinemaId(cinemaId);
        return halls.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("name", h.getName());
            map.put("seatRows", h.getSeatRows());
            map.put("seatCols", h.getSeatCols());
            map.put("hallType", h.getHallType().name());
            map.put("seatCount", h.getSeatRows() * h.getSeatCols());
            return map;
        }).toList();
    }
}
