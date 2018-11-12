package com.winstar.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;


/**
 * 验签
 */
@Slf4j
public class SignUtils {

	public static boolean validateSign(String param, String sign, String secret) {
		String key = param + secret;
		String paramSign = DigestUtils.md5DigestAsHex(key.getBytes());
		return sign.equals(paramSign);
	}

}
