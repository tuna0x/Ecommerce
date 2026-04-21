package com.tuna.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
 
 import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Slf4j
public class EcommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerceApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		log.info("\u003e\u003e\u003e App TimeZone set to Asia/Ho_Chi_Minh. Current time: {}", java.time.LocalDateTime.now());
	}

}
