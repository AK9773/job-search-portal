package com.ak.service;

public interface RedisService {

	public <T> void setValue(String key, T value, Long t);

	public <T> T getValue(String key, Class<T> entityClass);

	public void deleteKey(String key);
	
	public void deleteKeysByPattern(String pattern);

}
