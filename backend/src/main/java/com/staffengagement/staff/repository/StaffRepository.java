package com.staffengagement.staff.repository;

import com.staffengagement.staff.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
}
