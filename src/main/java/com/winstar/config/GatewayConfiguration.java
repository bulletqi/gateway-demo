package com.winstar.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.UUID;

@Slf4j
@Configuration
public class GatewayConfiguration {

	/**
	 * traceId签发
	 *
	 * @return globalFilter
	 */
	@Bean
	@Order
	public GlobalFilter traceIdFilter() {
		return (exchange, chain) -> {
			String traceId = UUID.randomUUID().toString();
			log.debug("new request traceId: [{}]", traceId);
			return chain.filter(exchange.mutate().request(
					exchange.getRequest().mutate().header("trace-id", traceId).build()
			).build());
		};
	}

}
