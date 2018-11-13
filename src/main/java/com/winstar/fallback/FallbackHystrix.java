package com.winstar.fallback;


import com.winstar.entity.CodeMessage;
import com.winstar.exception.ExceptionCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class FallbackHystrix {

	/**
	 * 熔断方法
	 * @return 错误信息
	 */
	@RequestMapping("/failback")
	public CodeMessage hystrixTimeout() {
		return ExceptionCodes.BUSINESS_TIMEOUT;
	}

}
