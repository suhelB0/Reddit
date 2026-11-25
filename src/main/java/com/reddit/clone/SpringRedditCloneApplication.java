package com.reddit.clone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SpringRedditCloneApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringRedditCloneApplication.class, args);
	}

}
