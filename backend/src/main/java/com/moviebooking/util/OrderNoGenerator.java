package com.moviebooking.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNoGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        long seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("ORD%s%04d", timestamp, seq);
    }
}
