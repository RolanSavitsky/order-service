package com.inno.orderservice.service;

import com.inno.orderservice.client.UserClient;
import com.inno.orderservice.dto.UserInfoResponse;
import com.inno.orderservice.exception.UserServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserClientService {

    private final UserClient userClient;

    @CircuitBreaker(name = "UserServiceCB", fallbackMethod = "getUserFallback")
    public UserInfoResponse getUserByEmail(String email) {
        return userClient.getUserByEmail(email);
    }

    private UserInfoResponse getUserFallback(String email, Throwable throwable) {
        if (throwable instanceof FeignException exception && exception.status() >= 400 && exception.status() < 500) {
            throw exception;
        }

        throw new UserServiceUnavailableException("User service is unavailable");
    }
}
