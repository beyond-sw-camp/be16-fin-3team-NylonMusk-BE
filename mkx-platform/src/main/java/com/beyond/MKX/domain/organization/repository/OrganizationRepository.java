package com.beyond.MKX.domain.organization.repository;

import com.beyond.MKX.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByRegNo (String regNo);
}