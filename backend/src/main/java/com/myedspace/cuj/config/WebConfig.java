package com.myedspace.cuj.config;

import com.myedspace.cuj.security.AuthInterceptor;
import com.myedspace.cuj.security.CurrentStudentArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * No CORS configuration here on purpose: the browser only ever talks to a same-origin server
 * (Vite's dev-server proxy locally, nginx in Docker — see vite.config.ts / frontend/nginx.conf),
 * so a permissive CORS policy would only widen the attack surface for no functional benefit.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentStudentArgumentResolver currentStudentArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/lms/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentStudentArgumentResolver);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
