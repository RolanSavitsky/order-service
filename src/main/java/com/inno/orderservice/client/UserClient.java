package com.inno.orderservice.client;

import com.inno.orderservice.dto.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${application.config.user-service-url}")
public interface UserClient {

    @GetMapping("/users/by-email")
    UserInfoResponse getUserByEmail(@RequestParam("email") String email);
}
