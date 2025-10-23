package com.beyond.MKX.domain.ipo.bookbuilding.repository;

import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IpoBookBuildingRepository extends JpaRepository<IpoBookBuilding, UUID> {
    List<IpoBookBuilding> findAllByIpoOffering_Id(UUID offeringId);
}
