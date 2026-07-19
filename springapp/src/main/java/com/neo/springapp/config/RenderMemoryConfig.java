package com.neo.springapp.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Disables auto-configurations that load extra beans and Metaspace-consuming classes
 * on Render's 512 MB free tier. Active only with {@code SPRING_PROFILES_ACTIVE=production}.
 */
@Configuration
@Profile("production")
@EnableAutoConfiguration(exclude = {
        JmxAutoConfiguration.class,
        SpringApplicationAdminJmxAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class
})
public class RenderMemoryConfig {
}
