//package com.cts.gemini_test_try2.config;
//
//import com.google.genai.Client;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class GeminiConfig {
//    @Value("#{'${google.api.key}'}")
//    private String apiKey;
//
//    @Bean
//    public Client geminiClient() {
//        // Force the environment variable into the JVM process
//        System.setProperty("GOOGLE_API_KEY", apiKey);
//        return new Client();
//    }
//}




package com.cts.gemini_test_try2.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    // Using the combined syntax we discussed
    @Value("#{'${google.api.key}'}")
    private String apiKey;

    @Bean
    public Client geminiClient() {

        System.out.println("---- GEMINI API KEY LOADED BY SPRING: [" + apiKey + "] ----");
        // Use the builder to explicitly set the key
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}