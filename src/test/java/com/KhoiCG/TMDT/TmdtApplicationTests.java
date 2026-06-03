package com.KhoiCG.TMDT;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;

@SpringBootTest
@ActiveProfiles("test")
class TmdtApplicationTests {

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
	}

}
