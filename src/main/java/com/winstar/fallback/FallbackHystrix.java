package com.winstar.fallback;


import com.winstar.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class FallbackHystrix {

	/**
	 * 熔断方法
	 *
	 * @return
	 */
	@RequestMapping("/failback")
	public Result hystrixTimeout() {
		return Result.builder().
				code(HttpStatus.BAD_REQUEST.value())
				.message("应用服务暂不可用").build();
	}

}
