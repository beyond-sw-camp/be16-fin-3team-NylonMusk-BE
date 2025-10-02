package com.beyond.MKX;

import com.beyond.MKX.common.auth.config.AuthCookieProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AuthCookieProperties.class)
@SpringBootApplication
public class MkxPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MkxPlatformApplication.class, args);
	}

}
