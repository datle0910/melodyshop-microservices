package com.melodyshop.product.config.feign;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ProductFeignConfig {

    @Bean
    public Request.Options options() {
        return new Request.Options(5, TimeUnit.SECONDS, 15, TimeUnit.SECONDS, true);
    }
}
