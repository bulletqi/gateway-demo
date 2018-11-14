package com.winstar.exception;


import com.winstar.entity.CodeMessage;
import lombok.Getter;

/**
 * 网关异常类
 */
public class GatewayException extends RuntimeException {

	@Getter
	private CodeMessage codeMessage;

	public GatewayException(Integer code, String message) {
		this(code, message, (Throwable) null);
	}

	public GatewayException(CodeMessage codeMessage) {
		this(codeMessage.getCode(), codeMessage.getMessage(), (Throwable) null);
		this.codeMessage = codeMessage;
	}

	public GatewayException(CodeMessage codeMessage, Throwable t) {
		this(codeMessage.getCode(), codeMessage.getMessage(), t);
	}

	public GatewayException(Integer code, String message, Throwable t) {
		super(message, t);
		this.codeMessage = new CodeMessage(code,message);
	}

}
