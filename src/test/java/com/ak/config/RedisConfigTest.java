package com.ak.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

	@Mock
	private RedisConnectionFactory redisConnectionFactory;

	@InjectMocks
	private RedisConfig redisConfig;

	private RedisTemplate<String, String> redisTemplate;

	@BeforeEach
	void setUp() {
		redisTemplate = redisConfig.redisTemplate(redisConnectionFactory);
		redisTemplate.afterPropertiesSet(); // Critical initialization
	}

	@Test
	void testRedisTemplateBeanCreation() {
		assertNotNull(redisTemplate, "RedisTemplate should not be null");
		assertSame(redisConnectionFactory, redisTemplate.getConnectionFactory(),
				"Should use the provided connection factory");

		assertTrue(redisTemplate.getKeySerializer() instanceof StringRedisSerializer,
				"Key serializer should be StringRedisSerializer");
		assertTrue(redisTemplate.getValueSerializer() instanceof StringRedisSerializer,
				"Value serializer should be StringRedisSerializer");
	}

	@Test
	void testRedisTemplateOperations() {
		// Mock the actual Redis operations
		when(redisConnectionFactory.getConnection()).thenReturn(mock(RedisConnection.class));

		// Test basic operations
		assertDoesNotThrow(() -> {
			redisTemplate.opsForValue().set("testKey", "testValue");
			redisTemplate.opsForValue().get("testKey");
		}, "Should support basic Redis operations");
	}
}