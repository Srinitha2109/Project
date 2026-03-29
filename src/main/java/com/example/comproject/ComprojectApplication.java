// package com.example.comproject;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// @org.springframework.scheduling.annotation.EnableScheduling
// public class ComprojectApplication {

// 	public static void main(String[] args) {
// 		SpringApplication.run(ComprojectApplication.class, args);
// 	}
// }

package com.example.comproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class ComprojectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComprojectApplication.class, args);
	}
}