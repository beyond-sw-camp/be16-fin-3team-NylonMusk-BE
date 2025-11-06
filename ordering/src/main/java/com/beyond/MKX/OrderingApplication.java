package com.beyond.MKX;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//@EnableScheduling - 카프카 소스커넥트 사용으로 인한 '비활성화'
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class OrderingApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderingApplication.class, args);
	}

}
