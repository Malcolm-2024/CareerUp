package io.careerup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CareerUpApplication {
  // Starts the web server that serves the built React pages and the /api database routes.
  public static void main(String[] args) {
    SpringApplication.run(CareerUpApplication.class, args);
  }
}
