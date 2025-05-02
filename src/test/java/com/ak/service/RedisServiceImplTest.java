package com.ak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class RedisServiceImplTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisServiceImpl redisService;

	private final String testKey = "testKey";
	private final Long expirationTime = 60L;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void getValue_ShouldReturnObject_WhenKeyExists() throws Exception {
		// Arrange
		TestObject expectedObject = new TestObject("test", 123);

		// Create a real ObjectMapper for JSON serialization
		ObjectMapper realMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		String json = realMapper.writeValueAsString(expectedObject);

		// Mock the RedisTemplate behavior
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(testKey)).thenReturn(json);

		// Act - Use the actual service instance (not redisService which might be a
		// different instance)
		TestObject result = redisService.getValue(testKey, TestObject.class);

		// Assert
		assertNotNull(result, "Returned object should not be null");
		assertEquals(expectedObject.getName(), result.getName(), "Name should match");
		assertEquals(expectedObject.getValue(), result.getValue(), "Value should match");

		// Verify interactions
		verify(redisTemplate).opsForValue();
		verify(valueOperations).get(testKey);
	}

	@Test
	void getValue_ShouldReturnNull_WhenKeyDoesNotExist() {
		// Arrange
		when(valueOperations.get(testKey)).thenReturn(null);

		// Act
		TestObject result = redisService.getValue(testKey, TestObject.class);

		// Assert
		assertNull(result);
	}

	@Test
	void getValue_ShouldReturnNull_WhenJsonProcessingExceptionOccurs() throws Exception {
		// Arrange
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		when(mockMapper.readValue(anyString(), eq(TestObject.class))).thenThrow(new JsonProcessingException("Error") {
		});

		RedisServiceImpl service = new RedisServiceImpl(redisTemplate);

		// Inject mock mapper via reflection
		Field mapperField = RedisServiceImpl.class.getDeclaredField("mapper");
		mapperField.setAccessible(true);
		mapperField.set(service, mockMapper);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(testKey)).thenReturn("invalid json");

		// Act
		TestObject result = service.getValue(testKey, TestObject.class);

		// Assert
		assertNull(result, "Should return null when JSON processing fails");
		verify(mockMapper).readValue(anyString(), eq(TestObject.class));
	}

	@Test
	void getListValue_ShouldReturnList_WhenKeyExists() throws Exception {
		// Arrange

		List<TestObject> expectedList = List.of(new TestObject("test1", 1), new TestObject("test2", 2));

		// Create a real ObjectMapper with the same configuration as production
		ObjectMapper testMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		String json = testMapper.writeValueAsString(expectedList);

		// Mock the RedisTemplate behavior
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(testKey)).thenReturn(json);

		// Act
		List<TestObject> result = redisService.getListValue(testKey, new TypeReference<List<TestObject>>() {
		});

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(expectedList.get(0).getName(), result.get(0).getName());
		assertEquals(expectedList.get(1).getValue(), result.get(1).getValue());
	}

	@Test
	void getListValue_ShouldReturnNull_WhenKeyDoesNotExist() {
		// Arrange
		when(valueOperations.get(testKey)).thenReturn(null);

		// Act
		List<TestObject> result = redisService.getListValue(testKey, new TypeReference<List<TestObject>>() {
		});

		// Assert
		assertNull(result);
	}

	@Test
	void setValue_ShouldStoreValueWithExpiration() throws Exception {
		// Arrange
		TestObject testObject = new TestObject("test", 123);
		String expectedJson = redisService.getMapper().writeValueAsString(testObject);

		// Act
		redisService.setValue(testKey, testObject, expirationTime);

		// Assert
		verify(valueOperations).set(testKey, expectedJson, expirationTime, TimeUnit.SECONDS);
	}

	@Test
	void setValue_ShouldHandleJsonProcessingException() throws Exception {
		ObjectMapper mockMapper = mock(ObjectMapper.class);
		when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error") {
		});

		RedisServiceImpl service = new RedisServiceImpl(redisTemplate);

		// Inject mock mapper via reflection
		Field mapperField = RedisServiceImpl.class.getDeclaredField("mapper");
		mapperField.setAccessible(true);
		mapperField.set(service, mockMapper);

		TestObject testObject = new TestObject("test", 123);

		// Act
		service.setValue(testKey, testObject, expirationTime);

		// Assert
		verify(mockMapper).writeValueAsString(testObject);
		verify(valueOperations, never()).set(any(), any(), anyLong(), any());
	}

	@Test
	void deleteKey_ShouldCallDelete() {
		// Act
		redisService.deleteKey(testKey);

		// Assert
		verify(redisTemplate).delete(testKey);
	}

	@Test
	void deleteKeysByPattern_ShouldDeleteKeys_WhenPatternMatches() {
		// Arrange
		String pattern = "test*";
		Set<String> keys = Set.of("test1", "test2");
		when(redisTemplate.keys(pattern)).thenReturn(keys);

		// Act
		redisService.deleteKeysByPattern(pattern);

		// Assert
		verify(redisTemplate).delete(keys);
	}

	@Test
	void deleteKeysByPattern_ShouldDoNothing_WhenNoKeysMatch() {
		// Arrange
		String pattern = "test*";
		when(redisTemplate.keys(pattern)).thenReturn(null);

		// Act
		redisService.deleteKeysByPattern(pattern);

		// Assert
		verify(redisTemplate, never()).delete(anySet());
	}

	// Helper class for testing
	private static class TestObject {
		private String name;
		private int value;

		public TestObject() {

		}

		public TestObject(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public int getValue() {
			return value;
		}
	}
}