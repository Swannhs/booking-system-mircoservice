package com.bookingapi;

import org.springframework.boot.SpringApplication;

public class TestBookingApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(BookingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
