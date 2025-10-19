package com.beyond.MKX.domain.ipo.allocation.service;

import com.beyond.MKX.domain.ipo.allocation.dto.StockUpdateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ordering-service")
public interface IpoAllocationFeign {


    @PutMapping("/stock/update")
    void applyStockUpdate(@RequestBody StockUpdateDTO dto);
}
