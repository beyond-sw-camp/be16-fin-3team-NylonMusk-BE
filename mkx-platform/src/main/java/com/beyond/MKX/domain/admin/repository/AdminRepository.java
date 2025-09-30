package com.beyond.MKX.domain.admin.repository;

import com.beyond.MKX.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    Optional<Admin> findByEmail (String email);
}
