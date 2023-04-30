package ru.itmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockMarketClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockMarketClientApplication.class, args).start();
    }
}
