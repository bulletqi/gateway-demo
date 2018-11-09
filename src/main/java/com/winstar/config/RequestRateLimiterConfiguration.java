package com.winstar.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 限流的策略
 * exchange对象中获取服务ID、请求信息，用户信息等
 */
@Configuration
public class RequestRateLimiterConfiguration {

	/**
	 * ip地址限流
	 *
	 * @return 限流key
	 */
	@Bean
	public KeyResolver remoteAddressKeyResolver() {
		return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getHostName());
	}

	/**
	 * 请求路径限流
	 *
	 * @return 限流key
	 */
	@Bean
	public KeyResolver apiKeyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getPath().value());
	}

	/**
	 * appId限流
	 *
	 * @return 限流key
	 */
	@Bean
	public KeyResolver userKeyResolver() {
		return exchange -> Mono.just(Objects.requireNonNull(
				exchange.getRequest().getHeaders().getFirst("appId")
		));
	}
}