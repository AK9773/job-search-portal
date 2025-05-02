package com.ak.service;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

public interface RedisService {

	public <T> void setValue(String key, T value, Long t);

	public <T> T getValue(String key, Class<T> entityClass);

	public void deleteKey(String key);

	public void deleteKeysByPattern(String pattern);

	public <T> List<T> getListValue(String key, TypeReference<List<T>> typeReference);

}
