package com.beyond.MKX.domain.disclosure.repository;

import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DisclosureRepository extends JpaRepository<Disclosure, UUID> {

}
