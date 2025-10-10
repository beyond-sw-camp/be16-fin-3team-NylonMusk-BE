package com.beyond.MKX;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableScheduling
@SpringBootApplication
@EnableFeignClients
public class OrderingApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderingApplication.class, args);
	}

}
