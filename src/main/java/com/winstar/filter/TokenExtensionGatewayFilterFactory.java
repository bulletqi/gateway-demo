package com.winstar.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winstar.entity.CodeMessage;
import com.winstar.exception.ExceptionCodes;
import com.winstar.exception.GatewayException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.util.ByteUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.function.Consumer;


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
				ServerHttpRequest request = exchange.getRequest();
				HttpHeaders headers = request.getHeaders();
				this.requestParamCheck(headers, request);
				String value = parseToken(headers);
				this.accountIdHandle(headers, value);
				return chain.filter(exchange);
			} catch (Exception e) {
				log.error("扩展过滤器异常", e);
				return buildResponse(exchange , e );
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
			throw new GatewayException(ExceptionCodes.TOKENERROR);
		}
		throw new GatewayException(ExceptionCodes.MISSTOKEN);
	}

	/**
	 * 请求参数校验
	 *
	 * @param headers 请求头
	 */
	private void requestParamCheck(HttpHeaders headers, ServerHttpRequest request) {
		String appId = headers.getFirst(APP_ID);
		if (StringUtils.isNotBlank(appId)) {
			String secureKey = redisTemplate.boundValueOps(REDIS_APPID_KEY + appId).get();
			if (StringUtils.isNotBlank(secureKey)) {
				//验证参数
				Flux<DataBuffer> body = request.getBody();
				DataBuffer dataBuffer = body.blockFirst();
			}
			throw new GatewayException(ExceptionCodes.GLOBALEXCEPTION);
		}
		throw new GatewayException(ExceptionCodes.MISSAPPID);
	}


	/**
	 * 验证签名
	 * @param param 提交参数
	 * @param sign 签名
	 * @param secret 密钥
	 * @return 是否通过
	 */
	public boolean validateSign(String param, String sign, String secret) {
		HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret);
		return sign.equals(hmacUtils.hmacHex(param));
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
	 *
	 * @param exchange exchange对象
	 * @return 异常信息
	 */
	private Mono<Void> buildResponse(ServerWebExchange exchange , Exception exception) {
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders httpHeaders = response.getHeaders();
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
		CodeMessage codeMessage;
		if(exception instanceof  GatewayException){
			codeMessage = ((GatewayException) exception).getCodeMessage();
		}else{
			codeMessage = ExceptionCodes.GLOBALEXCEPTION;
		}
		ObjectMapper mapper = new ObjectMapper();
		DataBuffer bodyDataBuffer;
		try {
			bodyDataBuffer = response.bufferFactory().wrap(mapper.writeValueAsBytes(codeMessage));
		} catch (JsonProcessingException e) {
			log.error("对象转换错误",e);
			return Mono.empty();
		}
		return response.writeWith(Mono.just(bodyDataBuffer));
	}

}
