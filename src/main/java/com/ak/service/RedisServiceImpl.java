package com.ak.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {

	private RedisTemplate redisTemplate;

	public RedisServiceImpl(RedisTemplate redisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
	}

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Override
	public <T> T getValue(String key, Class<T> entityClass) {

		try {
			Object object = redisTemplate.opsForValue().get(key);
			if (object == null) {
				return null;
			}
			return mapper.readValue(object.toString(), entityClass);
		} catch (Exception e) {
			log.error("Redis read error for key {}: {}", key, e.getMessage());
			return null;

		}
	}

	@Override
	public <T> void setValue(String key, T value, Long t) {

		try {
			String asString = mapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, asString, t, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("Exception: " + e.getMessage());
		}
	}

	@Override
	public void deleteKey(String key) {
		redisTemplate.delete(key);

	}

	@Override
	public void deleteKeysByPattern(String pattern) {
		Set<String> keys = redisTemplate.keys(pattern);
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}

}
