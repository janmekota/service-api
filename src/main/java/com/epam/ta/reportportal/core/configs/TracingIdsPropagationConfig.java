package com.epam.ta.reportportal.core.configs;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TracingIdsPropagationConfig implements WebMvcConfigurer {

  @Autowired
  private org.springframework.cloud.sleuth.Tracer tracer;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {

      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional.ofNullable(tracer.currentSpan())
                .ifPresent(span -> {
                  request.setAttribute("traceId", span.context().traceId());
                  request.setAttribute("spanId", span.context().spanId());
                });
        return true;
      }
    });
  }
}
