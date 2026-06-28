package com.ecommerce.strangler.proxy;

import com.ecommerce.strangler.routing.RoutingDecision;
import com.ecommerce.strangler.routing.StranglerRouter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The HTTP-level implementation of the Strangler Fig pattern.
 *
 * This filter sits at the edge of the proxy application.  Every incoming
 * HTTP request passes through it exactly once (guaranteed by
 * {@link OncePerRequestFilter}).
 *
 * What it does
 * ------------
 * 1. Asks {@link StranglerRouter} which backend should handle this request.
 * 2. Forwards the request (headers + body) to that backend via HTTP.
 * 3. Streams the backend's response back to the original caller.
 *
 * The caller (browser, mobile app, another service) is completely unaware of
 * which backend processed the request.  This transparent interception is the
 * key mechanism that makes the gradual migration safe: clients never need to
 * change their URLs.
 *
 * Error handling
 * --------------
 * If the target backend is unreachable or returns a 4xx/5xx, the proxy
 * propagates the status code faithfully so clients observe the real error.
 */
public class StranglerFigFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(StranglerFigFilter.class);

    private final StranglerRouter router;
    private final RestTemplate restTemplate;

    public StranglerFigFilter(StranglerRouter router, RestTemplate restTemplate) {
        this.router = router;
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        RoutingDecision decision = router.route(path, method);
        String targetUrl = decision.buildTargetUrl(path, request.getQueryString());

        log.info("[StranglerFig] {} {} → {} | target={}{}",
                method, path,
                decision.backend().getDisplayName(),
                targetUrl,
                decision.isDefaultFallback() ? " (default fallback)" : "");

        try {
            HttpHeaders headers = copyRequestHeaders(request);
            byte[] requestBody = request.getInputStream().readAllBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(
                    requestBody.length > 0 ? requestBody : null,
                    headers
            );

            ResponseEntity<byte[]> backendResponse = restTemplate.exchange(
                    URI.create(targetUrl),
                    HttpMethod.valueOf(method),
                    entity,
                    byte[].class
            );

            writeResponse(response, backendResponse);

        } catch (HttpStatusCodeException ex) {
            // The backend returned a 4xx/5xx — forward it faithfully
            response.setStatus(ex.getStatusCode().value());
            ex.getResponseHeaders().forEach((name, values) ->
                    values.forEach(value -> response.addHeader(name, value)));
            byte[] body = ex.getResponseBodyAsByteArray();
            if (body.length > 0) {
                response.getOutputStream().write(body);
            }
        } catch (Exception ex) {
            log.error("[StranglerFig] Failed to proxy {} {} to {}: {}",
                    method, path, targetUrl, ex.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "Backend '" + decision.backend().getDisplayName() + "' is unreachable");
        }
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            // Skip hop-by-hop headers that must not be forwarded
            if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("connection")) {
                headers.set(name, request.getHeader(name));
            }
        }
        return headers;
    }

    private void writeResponse(HttpServletResponse response,
                               ResponseEntity<byte[]> backendResponse) throws IOException {
        response.setStatus(backendResponse.getStatusCode().value());
        backendResponse.getHeaders().forEach((name, values) -> {
            // Transfer-Encoding is managed by the servlet container
            if (!name.equalsIgnoreCase("Transfer-Encoding")) {
                values.forEach(value -> response.addHeader(name, value));
            }
        });
        byte[] body = backendResponse.getBody();
        if (body != null && body.length > 0) {
            response.getOutputStream().write(body);
        }
    }
}
