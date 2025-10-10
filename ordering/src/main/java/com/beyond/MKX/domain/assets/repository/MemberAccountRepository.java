package com.beyond.MKX.domain.assets.repository;


import com.beyond.MKX.domain.assets.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberAccountRepository extends JpaRepository<MemberAccount, UUID> {



}
