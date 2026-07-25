package com.itpassport.app.repository;

import com.itpassport.app.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGuestToken(UUID guestToken);

    @Query("SELECT u FROM User u WHERE u.userType = com.itpassport.app.entity.UserType.GUEST AND u.lastActiveAt < :threshold")
    List<User> findExpiredGuests(@Param("threshold") LocalDateTime threshold);
}
