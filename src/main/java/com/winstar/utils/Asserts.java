package com.winstar.utils;

import com.winstar.entity.CodeMessage;
import com.winstar.exception.GatewayException;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

public class Asserts {

	public static void isNotBlank(String value, CodeMessage codeMessage) {
		if (StringUtils.isBlank(value)) {
			throw new GatewayException(codeMessage);
		}
	}

	public static void isNotNull(Object value, CodeMessage codeMessage) {
		if (value == null) {
			throw new GatewayException(codeMessage);
		}
	}

	public static void isTrue(Boolean value, CodeMessage codeMessage) {
		if (BooleanUtils.isNotTrue(value)) {
			throw new GatewayException(codeMessage);
		}
	}

}
