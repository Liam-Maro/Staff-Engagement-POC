package com.staffengagement.staff.repository;

import com.staffengagement.staff.model.RefreshToken;
import com.staffengagement.staff.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.staff = :staff")
    void deleteAllByStaff(Staff staff);
}
