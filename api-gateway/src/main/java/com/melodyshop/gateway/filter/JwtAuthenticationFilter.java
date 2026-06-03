package com.melodyshop.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Global JWT authentication filter for API Gateway.
 * Public endpoints are excluded from authentication.
 * After validation, user info is forwarded as headers
 * to downstream services.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Endpoints that bypass JWT check for any HTTP method.
     */
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/face/login",
            "/api/auth/register",
            "/api/auth/send-verification",
            "/api/auth/forgot-password",
            "/api/auth/refresh",
            "/api/auth/revoke-tokens",
            "/api/payments/webhook",
            "/api/payments/v3/api-docs",
            "/api/payments/swagger-ui",
            "/api/engagement/v3/api-docs",
            "/api/engagement/swagger-ui",
            "/api/orders/has-orders",       // Internal service check
            "/api/orders/has-product-orders", // Internal service check
            "/api/orders/quote",             // Public authoritative checkout quote
            "/api/orders/guest",             // Public guest checkout endpoint
            "/api/inventory/check",          // Public stock check endpoint
            "/api/media/proxy",              // Proxy image requests to bypass browser tracking prevention
            "/api/ai/",                      // AI Chat - public for all users
            "/api/ai/v1/",                   // AI Chat v1 - public for all users
            "/eureka"
    );

    /**
     * Endpoints that bypass JWT check ONLY for GET and OPTIONS HTTP methods.
     */
    private static final List<String> PUBLIC_GET_ONLY_ENDPOINTS = List.of(
            "/api/products",     // Allow public catalog browsing
            "/api/categories",   // Allow category listing
            "/api/brands"        // Allow brand listing
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = validateToken(token);
            String role = claims.get("role", String.class);
            if (path.startsWith("/api/admin/") && !isAdmin(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-FullName", claims.get("fullName", String.class))
                    .header("X-User-Phone", claims.get("phone", String.class))
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicEndpoint(String path, String method) {
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            if (PUBLIC_GET_ONLY_ENDPOINTS.stream().anyMatch(path::startsWith)) {
                return true;
            }
        }
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public int getOrder() {
        return -1; // Highest priority - run before all other filters
    }
}
