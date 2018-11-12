package com.winstar.filter;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * token验证合法性及参数验参校验
 */
@Slf4j
@Component
public class TokenExtensionGatewayFilterFactory extends AbstractGatewayFilterFactory {

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String APP_ID = "appId";
	private static final String TOKEN_NAME = "token";
	private static final String ACCOUNT_ID = "accountId";
	private static final String REDIS_TOKEN_KEY = "token:";
	private static final String REDIS_APPID_KEY = "appId:";

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			try {
				log.debug("token验证合法性及参数验参校验filter");
				HttpHeaders headers = exchange.getRequest().getHeaders();
				this.requestParamCheck(headers);
				String value = parseToken(headers);
				this.accountIdHandle(headers, value);
				return chain.filter(exchange);
			} catch (Exception e) {
				log.error("扩展过滤器异常", e);
				return buildResponse(exchange);
			}
		};
	}

	/**
	 * 解析token值
	 *
	 * @param headers 请求头
	 * @return 返回token对应的值
	 */
	private String parseToken(HttpHeaders headers) {
		String token = headers.getFirst(TOKEN_NAME);
		if (StringUtils.isNotBlank(token)) {
			String value = redisTemplate.boundValueOps(REDIS_TOKEN_KEY + token).get();
			if (StringUtils.isNotBlank(value)) {
				return value;
			}
			throw new RuntimeException("");
		}
		throw new RuntimeException("");
	}

	/**
	 * 请求参数校验
	 *
	 * @param headers 请求头
	 */
	private void requestParamCheck(HttpHeaders headers) {
		String appId = headers.getFirst(APP_ID);
		if (StringUtils.isNotBlank(appId)) {
			String secureKey = redisTemplate.boundValueOps(REDIS_APPID_KEY + appId).get();
			if (StringUtils.isNotBlank(secureKey)) {
				//验证参数

			}
			throw new RuntimeException("");
		}
		throw new RuntimeException("");
	}

	/**
	 * 处理accountId
	 *
	 * @param headers 请求头
	 * @param value   token对应的value
	 */
	private void accountIdHandle(HttpHeaders headers, String value) {
		String accountId = value;
		headers.set(ACCOUNT_ID, accountId);
	}

	/**
	 * 异常时，处理内容
	 * @param exchange exchange对象
	 * @return 异常信息
	 */
	private Mono<Void> buildResponse(ServerWebExchange exchange) {
		return Mono.empty();
	}

}
