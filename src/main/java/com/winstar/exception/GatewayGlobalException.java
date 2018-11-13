package com.winstar.exception;

import com.winstar.entity.CodeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;


/**
 * 自定义返回异常,网关统一异常处理
 */
@Slf4j
public class GatewayGlobalException extends DefaultErrorWebExceptionHandler {

	/**
	 * Create a new {@code DefaultErrorWebExceptionHandler} instance.
	 *
	 * @param errorAttributes    the error attributes
	 * @param resourceProperties the resources configuration properties
	 * @param errorProperties    the error configuration properties
	 * @param applicationContext the current application context
	 */
	public GatewayGlobalException(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ErrorProperties errorProperties, ApplicationContext applicationContext) {
		super(errorAttributes, resourceProperties, errorProperties, applicationContext);
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
	}

	@Override
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		Map<String, Object> error = getErrorAttributes(request, isIncludeStackTrace(request, MediaType.ALL));
		HttpStatus errorStatus = getHttpStatus(error);
		Throwable throwable = getError(request);
		return ServerResponse.status(errorStatus)
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(exceptionHandle(throwable)))
				.doOnNext((resp) -> logError(request, errorStatus));
	}

	private CodeMessage exceptionHandle(Throwable throwable) {
		log.error("网关统一异常拦截", throwable);
		return ExceptionCodes.GLOBALEXCEPTION;
	}

}
