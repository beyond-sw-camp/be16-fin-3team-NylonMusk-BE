package com.beyond.MKX.domain.outbox.repository;

import com.beyond.MKX.domain.outbox.entity.OrderOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

    /**
     * 발행되지 않은 Outbox 이벤트를 생성 시간 순서대로 N개 조회합니다.
     * PESSIMISTIC_WRITE 락을 사용하여 다른 트랜잭션이 동일한 레코드를 조회하는 것을 방지합니다.
     * (데이터베이스는 FOR UPDATE SKIP LOCKED 옵션을 지원해야 합니다)
     *
     * @param pageable 조회할 개수(N)를 담은 Pageable 객체
     * @return 조회된 OrderOutbox 리스트
     */
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT o FROM OrderOutbox o WHERE o.isPublished = false ORDER BY o.createdAt ASC")
//    List<OrderOutbox> findUnpublishedBatch(Pageable pageable);

//    @Query(
//            value = """
//        SELECT *
//        FROM order_outbox
//        WHERE is_published = false
//        ORDER BY created_at ASC
//        LIMIT :#{#pageable.pageSize}
//        OFFSET :#{#pageable.offset}
//        FOR UPDATE SKIP LOCKED
//        """,
//            nativeQuery = true
//    )
//    List<OrderOutbox> findUnpublishedBatchSkipLocked(Pageable pageable);
}
