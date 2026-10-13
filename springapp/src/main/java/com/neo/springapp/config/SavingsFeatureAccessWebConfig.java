package com.neo.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SavingsFeatureAccessWebConfig implements WebMvcConfigurer {
    private final SavingsFeatureAccessInterceptor featureAccessInterceptor;

    public SavingsFeatureAccessWebConfig(SavingsFeatureAccessInterceptor featureAccessInterceptor) {
        this.featureAccessInterceptor = featureAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureAccessInterceptor).addPathPatterns("/api/**");
    }
}
