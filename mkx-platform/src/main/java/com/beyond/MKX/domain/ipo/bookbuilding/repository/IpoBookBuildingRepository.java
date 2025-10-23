package com.beyond.MKX.domain.ipo.bookbuilding.repository;

import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IpoBookBuildingRepository extends JpaRepository<IpoBookBuilding, UUID> {
    List<IpoBookBuilding> findAllByIpoOffering_Id(UUID offeringId);

    List<IpoBookBuilding> findAllByIpoOffering_IpoOfferingStatus(IpoOfferingStatus ipoOfferingStatus);

    boolean existsByIpoOffering_IdAndParticipantId(UUID ipoOfferingId, UUID participantId);
}
