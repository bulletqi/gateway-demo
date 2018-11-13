package com.winstar.exception;

import com.winstar.entity.CodeMessage;

/**
 * 异常错误码
 */
public class ExceptionCodes {

    public static final CodeMessage GLOBALEXCEPTION = new CodeMessage(201,"网关服务暂不可用");

    public static final CodeMessage BUSINESSTIMEOUT = new CodeMessage(202,"应用服务暂不可用");

    public static final CodeMessage MISSAPPID = new CodeMessage(300,"请求头缺少appId参数");

    public static final CodeMessage MISSTOKEN = new CodeMessage(301,"请求头缺少token参数");

    public static final CodeMessage TOKENERROR = new CodeMessage(302,"token错误或已过期");

}
