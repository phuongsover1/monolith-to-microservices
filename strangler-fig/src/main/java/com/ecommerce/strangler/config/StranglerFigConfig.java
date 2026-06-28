package com.ecommerce.strangler.config;

import com.ecommerce.strangler.proxy.StranglerFigFilter;
import com.ecommerce.strangler.routing.BackendType;
import com.ecommerce.strangler.routing.RoutingRule;
import com.ecommerce.strangler.routing.StranglerRouter;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Wires up all Strangler Fig beans from the {@link StranglerProperties} values.
 */
@Configuration
@EnableConfigurationProperties(StranglerProperties.class)
public class StranglerFigConfig {

    @Bean
    public StranglerRouter stranglerRouter(StranglerProperties props) {
        List<RoutingRule> rules = props.routes().stream()
                .map(r -> new RoutingRule(r.pathPattern(), r.httpMethod(), r.target()))
                .toList();

        Map<BackendType, String> backendUrls = Map.of(
                BackendType.MONOLITH, props.monolithUrl(),
                BackendType.INVENTORY_SERVICE, props.inventoryServiceUrl()
        );

        return new StranglerRouter(rules, backendUrls);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Registers the proxy filter to intercept every request under /api/**.
     * The filter is the HTTP entry point of the Strangler Fig proxy.
     */
    @Bean
    public FilterRegistrationBean<StranglerFigFilter> stranglerFigFilter(
            StranglerRouter router,
            RestTemplate restTemplate
    ) {
        FilterRegistrationBean<StranglerFigFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new StranglerFigFilter(router, restTemplate));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        registration.setName("stranglerFigFilter");
        return registration;
    }
}
