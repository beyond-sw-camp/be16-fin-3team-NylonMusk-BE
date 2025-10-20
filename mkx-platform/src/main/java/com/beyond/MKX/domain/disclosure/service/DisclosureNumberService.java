package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.domain.disclosure.entity.DisclosureSequence;
import com.beyond.MKX.domain.disclosure.repository.DisclosureSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DisclosureNumberService {

    private final DisclosureSequenceRepository sequenceRepository;

    @Transactional
    public String issueNumber() {
        int year = LocalDate.now().getYear();
        DisclosureSequence seq = sequenceRepository.findByYear(year)
                .orElseGet(() -> sequenceRepository.save(
                        DisclosureSequence.builder().year(year).currentNo(0).build()
                ));
        int no = seq.nextAndGet();
        return String.format("%04d-%05d호", year, no);
    }
}

