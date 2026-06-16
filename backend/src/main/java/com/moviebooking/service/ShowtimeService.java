package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.dto.ShowtimeRequest;
import com.moviebooking.dto.BatchShowtimeRequest;
import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final HallRepository hallRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public ShowtimeService(ShowtimeRepository showtimeRepository, HallRepository hallRepository,
                           CinemaRepository cinemaRepository, SeatRepository seatRepository,
                           MovieRepository movieRepository) {
        this.showtimeRepository = showtimeRepository;
        this.hallRepository = hallRepository;
        this.cinemaRepository = cinemaRepository;
        this.seatRepository = seatRepository;
        this.movieRepository = movieRepository;
    }

    public List<Map<String, Object>> getShowtimeList(Long movieId, Long cinemaId, String dateStr) {
        LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();
        List<Showtime> showtimes = showtimeRepository.findByMovieIdAndCinemaIdAndDate(movieId, cinemaId, date);

        // 按影院分组
        Map<Long, List<Showtime>> grouped = new LinkedHashMap<>();
        for (Showtime s : showtimes) {
            Hall hall = hallRepository.findById(s.getHallId()).orElse(null);
            if (hall == null) continue;
            grouped.computeIfAbsent(hall.getCinemaId(), k -> new ArrayList<>()).add(s);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Showtime>> entry : grouped.entrySet()) {
            Cinema cinema = cinemaRepository.findById(entry.getKey()).orElse(null);
            if (cinema == null) continue;

            Map<String, Object> cinemaMap = new HashMap<>();
            cinemaMap.put("cinemaId", cinema.getId());
            cinemaMap.put("cinemaName", cinema.getName());

            List<Map<String, Object>> showtimeList = new ArrayList<>();
            for (Showtime s : entry.getValue()) {
                Hall hall = hallRepository.findById(s.getHallId()).orElse(null);
                if (hall == null) continue;

                int totalSeats = seatRepository.findByHallIdOrderByRowNumAscColNumAsc(hall.getId()).size();
                List<Long> soldSeatIds = seatRepository.findSoldSeatIdsByShowtimeId(s.getId());

                Map<String, Object> st = new HashMap<>();
                st.put("id", s.getId());
                st.put("hallId", hall.getId());
                st.put("hallName", hall.getName());
                st.put("hallType", hall.getHallType().name());
                st.put("showDate", s.getShowDate());
                st.put("showTime", s.getShowTime());
                st.put("price", s.getPrice());
                st.put("availableSeats", totalSeats - soldSeatIds.size());
                st.put("totalSeats", totalSeats);
                st.put("status", "available");
                showtimeList.add(st);
            }
            cinemaMap.put("showtimes", showtimeList);
            result.add(cinemaMap);
        }

        return result;
    }

    public Map<String, Object> getSeatMap(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("场次不存在"));

        Hall hall = hallRepository.findById(showtime.getHallId())
                .orElseThrow(() -> BusinessException.notFound("影厅不存在"));

        List<Seat> seats = seatRepository.findByHallIdOrderByRowNumAscColNumAsc(hall.getId());
        List<Long> soldSeatIds = seatRepository.findSoldSeatIdsByShowtimeId(showtimeId);

        List<Map<String, Object>> seatList = new ArrayList<>();
        for (Seat seat : seats) {
            Map<String, Object> seatMap = new HashMap<>();
            seatMap.put("id", seat.getId());
            seatMap.put("row", seat.getRowNum());
            seatMap.put("col", seat.getColNum());
            seatMap.put("type", seat.getSeatType().name());

            if (seat.getStatus() == com.moviebooking.entity.enums.SeatStatus.maintenance) {
                seatMap.put("status", "locked");
            } else if (soldSeatIds.contains(seat.getId())) {
                seatMap.put("status", "sold");
            } else {
                seatMap.put("status", "available");
            }
            seatList.add(seatMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("showtimeId", showtimeId);
        result.put("hallName", hall.getName());
        result.put("hallType", hall.getHallType().name());
        result.put("rows", hall.getSeatRows());
        result.put("cols", hall.getSeatCols());
        result.put("seats", seatList);
        return result;
    }

    // --- Admin methods ---

    @Transactional
    public Map<String, Object> createShowtime(ShowtimeRequest request) {
        Showtime showtime = new Showtime();
        showtime.setMovieId(request.getMovieId());
        showtime.setHallId(request.getHallId());
        showtime.setShowDate(request.getShowDate());
        showtime.setShowTime(request.getShowTime());
        showtime.setPrice(request.getPrice());
        showtime = showtimeRepository.save(showtime);
        return Map.of("id", showtime.getId());
    }

    @Transactional
    public List<Map<String, Object>> batchCreateShowtime(BatchShowtimeRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (var time : request.getTimes()) {
            Showtime showtime = new Showtime();
            showtime.setMovieId(request.getMovieId());
            showtime.setHallId(request.getHallId());
            showtime.setShowDate(request.getShowDate());
            showtime.setShowTime(time);
            showtime.setPrice(request.getPrice());
            showtime = showtimeRepository.save(showtime);
            results.add(Map.of("id", showtime.getId(), "showTime", time));
        }
        return results;
    }
}
