package com.interior.platform.config;

import com.interior.platform.security.interceptor.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final com.interior.platform.operations.interceptor.MaintenanceModeInterceptor maintenanceModeInterceptor;

    @Value("${app.security.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public WebMvcConfig(
            SecurityInterceptor securityInterceptor,
            com.interior.platform.operations.interceptor.MaintenanceModeInterceptor maintenanceModeInterceptor
    ) {
        this.securityInterceptor = securityInterceptor;
        this.maintenanceModeInterceptor = maintenanceModeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/**");

        registry.addInterceptor(maintenanceModeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
