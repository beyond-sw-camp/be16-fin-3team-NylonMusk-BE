package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.domain.execution.entity.FillLog;
import com.beyond.MKX.domain.execution.repository.FillLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AskExecutionService {

    private final FillLogRepository fillLogRepository;

    public boolean askExecuteProcess(UUID askOrderId, ExecutionEvent executionEvent) {

        // 0. 멱등성 검사
        System.out.println("===========멱등성 검사 시작===========");
        boolean b = fillLogRepository.existsByOrderLogIdAndExecId(askOrderId, executionEvent.getExecId());
        if (b) {
            System.out.println("=========== 이미 있는 멱등성 입니다.  ===========");
            return true;
        } else {
            fillLogRepository.save(
                    FillLog.builder()
                            .orderLogId(askOrderId)
                            .execId(executionEvent.getExecId())
                            .ticker(executionEvent.getTicker())
                            .side(executionEvent.getSide())
                            .price(executionEvent.getPrice())
                            .quantity(executionEvent.getQuantity())
                            .build()
            );
        }
        System.out.println("=========== 신규 등록 ===========");










        return true;
    }
}
