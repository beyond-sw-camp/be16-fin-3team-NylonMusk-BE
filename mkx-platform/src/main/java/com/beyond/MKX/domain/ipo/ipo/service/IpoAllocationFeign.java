package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.domain.ipo.ipo.dto.StockUpdateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ordering-service")
public interface IpoAllocationFeign {


    @PutMapping("/my-stocks/stock/update")
    void applyStockUpdate(@RequestBody StockUpdateDTO dto);
}
