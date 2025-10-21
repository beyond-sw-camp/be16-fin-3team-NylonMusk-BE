package com.beyond.MKX.domain.assets.service;

import com.beyond.MKX.domain.assets.config.FeignAuthConfig;
import com.beyond.MKX.domain.assets.dto.AccountIdResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "mkx-platform-service", configuration = FeignAuthConfig.class)
public interface AccountFeign {
    @GetMapping("/my-stocks/{corpId}/account-brief")
    AccountIdResDTO getAccountId(@PathVariable("corpId") UUID corpId);

}
