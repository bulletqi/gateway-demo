package com.winstar.exception;

import com.winstar.entity.CodeMessage;

/**
 * 异常错误码
 */
public class ExceptionCodes {

    public static final CodeMessage GLOBAL_EXCEPTION = new CodeMessage(201,"网关服务暂不可用");

    public static final CodeMessage BUSINESS_TIMEOUT = new CodeMessage(202,"应用服务请求超时");

    public static final CodeMessage MISS_APPID = new CodeMessage(300,"请求头缺少appId参数");

    public static final CodeMessage MISS_TOKEN = new CodeMessage(301,"请求头缺少token参数");

    public static final CodeMessage TOKEN_ERROR = new CodeMessage(302,"token错误或已过期");

    public static final CodeMessage MISS_SECUREKEY = new CodeMessage(303,"缺少密钥");

    public static final CodeMessage MISS_SIGN = new CodeMessage(304,"请求头缺少签名");

    public static final CodeMessage SIGN_ERROR = new CodeMessage(305,"签名异常");

    public static final CodeMessage MISS_CONTENTTYPE = new CodeMessage(306,"请求头缺少tontent-type");

}
