package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.dto.MovieRequest;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public PageResult<Map<String, Object>> getMovieList(String status, String keyword, int page, int size) {
        MovieStatus movieStatus = null;
        if (status != null && !status.isEmpty()) {
            movieStatus = MovieStatus.valueOf(status);
        }

        Page<Movie> moviePage = movieRepository.findByStatusAndKeyword(
                movieStatus, keyword, PageRequest.of(page - 1, size, Sort.by("id").descending()));

        var list = moviePage.getContent().stream().map(this::toMovieBrief).toList();
        return new PageResult<>(list, moviePage.getTotalElements(), page, size);
    }

    public Map<String, Object> getMovieDetail(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("电影不存在"));
        return toMovieDetail(movie);
    }

    // --- Admin methods ---

    public Map<String, Object> createMovie(MovieRequest request) {
        Movie movie = new Movie();
        applyMovieFields(movie, request);
        movie = movieRepository.save(movie);
        return Map.of("id", movie.getId());
    }

    public void updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("电影不存在"));
        applyMovieFields(movie, request);
        movieRepository.save(movie);
    }

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw BusinessException.notFound("电影不存在");
        }
        movieRepository.deleteById(id);
    }

    private void applyMovieFields(Movie movie, MovieRequest req) {
        if (req.getTitle() != null) movie.setTitle(req.getTitle());
        if (req.getDuration() != null) movie.setDuration(req.getDuration());
        if (req.getReleaseDate() != null) movie.setReleaseDate(req.getReleaseDate());
        if (req.getGenre() != null) movie.setGenre(req.getGenre());
        if (req.getDirector() != null) movie.setDirector(req.getDirector());
        if (req.getActors() != null) movie.setActors(req.getActors());
        if (req.getDescription() != null) movie.setDescription(req.getDescription());
        if (req.getPoster() != null) movie.setPoster(req.getPoster());
        if (req.getRating() != null) movie.setRating(req.getRating());
        if (req.getStatus() != null) movie.setStatus(MovieStatus.valueOf(req.getStatus()));
    }

    private Map<String, Object> toMovieBrief(Movie m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("title", m.getTitle());
        map.put("poster", m.getPoster());
        map.put("duration", m.getDuration());
        map.put("releaseDate", m.getReleaseDate());
        map.put("rating", m.getRating());
        map.put("status", m.getStatus().name());
        map.put("genre", m.getGenre());
        return map;
    }

    private Map<String, Object> toMovieDetail(Movie m) {
        Map<String, Object> map = toMovieBrief(m);
        map.put("director", m.getDirector());
        map.put("actors", m.getActors());
        map.put("description", m.getDescription());
        return map;
    }
}
