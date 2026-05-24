package com.cts.sarreport.exception;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String city) {

        super("No SAR data available for the city: " + city);
    }
}
