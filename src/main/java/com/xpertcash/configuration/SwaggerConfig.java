package com.xpertcash.configuration;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiVetCareOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("XpertCash Api")
                        .description("Une api XpertCash pour gérer les entreprises")
                        .version("2.6.0"));
    }
}