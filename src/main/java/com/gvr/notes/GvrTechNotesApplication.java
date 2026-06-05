	package com.gvr.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GvrTechNotesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GvrTechNotesApplication.class, args);
	}

}
	