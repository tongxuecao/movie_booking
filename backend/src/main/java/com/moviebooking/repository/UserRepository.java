package com.moviebooking.repository;

import com.moviebooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.walletBalance = u.walletBalance - :amount, u.version = u.version + 1 WHERE u.id = :userId AND u.version = :version AND u.walletBalance >= :amount")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.walletBalance = u.walletBalance + :amount, u.version = u.version + 1 WHERE u.id = :userId")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
