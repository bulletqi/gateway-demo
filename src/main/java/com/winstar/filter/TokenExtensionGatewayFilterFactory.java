package com.winstar.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winstar.entity.CodeMessage;
import com.winstar.exception.ExceptionCodes;
import com.winstar.exception.GatewayException;
import com.winstar.utils.Asserts;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;


/**
 * token验证合法性及参数验参校验
 */
@Slf4j
@Component
public class TokenExtensionGatewayFilterFactory extends AbstractGatewayFilterFactory {

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String HEADER_APP_ID = "appId";
	private static final String HEADER_TOKEN_NAME = "token";
	private static final String HEADER_SIGN = "sign";
	private static final String HEADER_ACCOUNT_ID = "accountId";
	private static final String REDIS_TOKEN_KEY = "token:";
	private static final String REDIS_APPID_KEY = "appId:";

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			try {
				log.debug("token验证合法性及参数验参校验filter");
				ServerHttpRequest request = exchange.getRequest();
				HttpHeaders headers = request.getHeaders();
				request = this.test(headers, request);
//				request = this.requestParamCheck(headers, request);
//				request = this.accountIdHeader(request, parseToken(headers));
				return chain.filter(exchange.mutate().request(request).build());
			} catch (Exception e) {
				log.error("扩展过滤器异常", e);
				return buildResponse(exchange, e);
			}
		};
	}

	private ServerHttpRequest test(HttpHeaders headers, final ServerHttpRequest request) {

		Flux<DataBuffer> body = request.getBody();
		AtomicReference<String> bodyRef = new AtomicReference<>();
		MediaType contentType = request.getHeaders().getContentType();

		contentType.includes(MediaType.MULTIPART_FORM_DATA);
		body.subscribe(buffer -> {
			byte[] bytes = new byte[buffer.readableByteCount()];
			buffer.read(bytes);
			DataBufferUtils.release(buffer);
			String requestParam = new String(bytes, StandardCharsets.UTF_8);
			log.debug("请求提交参数:[{}]", requestParam);
			bodyRef.set(requestParam);
		});

		if (StringUtils.isNotBlank(bodyRef.get())) {
			return new ServerHttpRequestDecorator(request) {
				@Override
				public Flux<DataBuffer> getBody() {
					return Flux.just(wrapDataBuffer(bodyRef.get()));
				}
			};
		} else {
			return request;
		}

	}

	/**
	 * 包装参数
	 *
	 * @param param 请求参数
	 * @return DataBuffer
	 */
	private DataBuffer wrapDataBuffer(String param) {
		NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
		return nettyDataBufferFactory.wrap(param.getBytes());
	}


	/**
	 * 解析token值
	 *
	 * @param headers 请求头
	 * @return 返回token对应的值
	 */
	private String parseToken(HttpHeaders headers) {
		String token = headers.getFirst(HEADER_TOKEN_NAME);
		Asserts.isNotBlank(token, ExceptionCodes.MISS_TOKEN);
		String value = redisTemplate.boundValueOps(REDIS_TOKEN_KEY + token).get();
		Asserts.isNotBlank(value, ExceptionCodes.TOKEN_ERROR);
		return value;
	}

	/**
	 * 请求参数校验
	 *
	 * @param headers 请求头
	 */
	private ServerHttpRequest requestParamCheck(HttpHeaders headers, ServerHttpRequest request) {
		String appId = headers.getFirst(HEADER_APP_ID);
		Asserts.isNotBlank(appId, ExceptionCodes.MISS_APPID);
		String secureKey = redisTemplate.boundValueOps(REDIS_APPID_KEY + appId).get();
		Asserts.isNotBlank(secureKey, ExceptionCodes.MISS_SECUREKEY);
		//验证参数
		String requestParam = parseRequestBody(request);
		MediaType contentType = request.getHeaders().getContentType();
		Asserts.isNotNull(contentType, ExceptionCodes.MISS_CONTENTTYPE);
		if (StringUtils.isNotBlank(requestParam)
				&& (contentType.includes(MediaType.APPLICATION_JSON) || contentType.includes(MediaType.TEXT_PLAIN))) {
			String sign = headers.getFirst(HEADER_SIGN);
			Asserts.isNotBlank(sign, ExceptionCodes.MISS_SIGN);
			Asserts.isTrue(validateSign(requestParam, sign, secureKey), ExceptionCodes.SIGN_ERROR);
		}
		//包装请求
		if (StringUtils.isNotBlank(requestParam)) {
			return new ServerHttpRequestDecorator(request) {
				@Override
				public Flux<DataBuffer> getBody() {
					return Flux.just(wrapDataBuffer(requestParam));
				}
			};
		}
		return request;
	}

	private String parseRequestBody(ServerHttpRequest request) {
		Flux<DataBuffer> body = request.getBody();
		AtomicReference<String> bodyRef = new AtomicReference<>();
		body.subscribe(buffer -> {
			byte[] bytes = new byte[buffer.readableByteCount()];
			buffer.read(bytes);
			DataBufferUtils.release(buffer);
			String requestParam = new String(bytes, StandardCharsets.UTF_8);
			log.debug("请求提交参数:[{}]", requestParam);
			bodyRef.set(requestParam);
		});
		return bodyRef.get();
	}

	/**
	 * 验证签名
	 *
	 * @param param  提交参数
	 * @param sign   签名
	 * @param secret 密钥
	 * @return 是否通过
	 */
	public boolean validateSign(String param, String sign, String secret) {
		HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret);
		return sign.equals(hmacUtils.hmacHex(param));
	}

	/**
	 * 将accounId设置到header中
	 *
	 * @param request   请求对象
	 * @param accountId accountId
	 */
	private ServerHttpRequest accountIdHeader(ServerHttpRequest request, String accountId) {
		return request.mutate().header(HEADER_ACCOUNT_ID, accountId).build();
	}

	/**
	 * 异常时，处理内容
	 *
	 * @param exchange exchange对象
	 * @return 异常信息
	 */
	private Mono<Void> buildResponse(ServerWebExchange exchange, Exception exception) {
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders httpHeaders = response.getHeaders();
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
		CodeMessage codeMessage;
		if (exception instanceof GatewayException) {
			codeMessage = ((GatewayException) exception).getCodeMessage();
		} else {
			codeMessage = ExceptionCodes.GLOBAL_EXCEPTION;
		}
		ObjectMapper mapper = new ObjectMapper();
		DataBuffer bodyDataBuffer;
		try {
			bodyDataBuffer = response.bufferFactory().wrap(mapper.writeValueAsBytes(codeMessage));
		} catch (JsonProcessingException e) {
			log.error("对象转换错误", e);
			return Mono.empty();
		}
		return response.writeWith(Mono.just(bodyDataBuffer));
	}

}
