package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.FillLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FillLogRepository extends JpaRepository<FillLog, UUID> {

    boolean existsByOrderLogIdAndExecId(UUID orderLogId, String execId);

}
