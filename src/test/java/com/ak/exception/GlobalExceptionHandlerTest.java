package com.ak.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ak.dto.ErrorResponse;
import com.ak.dto.ValidationErrorResponse;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	@InjectMocks
	private GlobalExceptionHandler globalExceptionHandler;

	private MethodArgumentNotValidException methodArgumentNotValidException;
	private BindingResult bindingResult;

	@BeforeEach
	void setUp() {
		bindingResult = mock(BindingResult.class);
		methodArgumentNotValidException = new MethodArgumentNotValidException(null, bindingResult);
	}

	@Test
	void handleResourceNotFound_ShouldReturnNotFoundResponse() {
		// Arrange
		String errorMessage = "Resource not found";
		ConfigDataResourceNotFoundException ex = mock(ConfigDataResourceNotFoundException.class);
		when(ex.getMessage()).thenReturn(errorMessage);

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(ex);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals(errorMessage, Objects.requireNonNull(response.getBody()).getMessage());
		assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void unauthorizedAccessExceptionHandler_ShouldReturnUnauthorizedResponse() {
		// Arrange
		String errorMessage = "Unauthorized access";
		UnauthorizedAccessException ex = new UnauthorizedAccessException(errorMessage);

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.unauthorizedAccessExceptionHandler(ex);

		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertEquals(errorMessage, Objects.requireNonNull(response.getBody()).getMessage());
		assertEquals(401, response.getBody().getStatus());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void resourceNotFoundExceptionHandler_ShouldReturnBadRequestResponse() {
		// Arrange
		String errorMessage = "Resource not found";
		ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.resourceNotFoundExceptionHandler(ex);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(errorMessage, Objects.requireNonNull(response.getBody()).getMessage());
		assertEquals(400, response.getBody().getStatus());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void noResourceFoundExceptionHandler_ShouldReturnBadRequestResponse() {
		// Arrange
		String errorMessage = "No resource found";
		NoResourceFoundException ex = mock(NoResourceFoundException.class);
		when(ex.getMessage()).thenReturn(errorMessage);

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.noResourceFoundExceptionHandler(ex);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals(errorMessage, Objects.requireNonNull(response.getBody()).getMessage());
		assertEquals(400, response.getBody().getStatus());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void handleValidationExceptions_ShouldReturnValidationErrorResponse() {
		// Arrange
		FieldError fieldError1 = new FieldError("objectName", "field1", "must not be null");
		FieldError fieldError2 = new FieldError("objectName", "field2", "must be positive");
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

		// Act
		ResponseEntity<ValidationErrorResponse> response = globalExceptionHandler
				.handleValidationExceptions(methodArgumentNotValidException);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		ValidationErrorResponse body = response.getBody();
		assertNotNull(body);
		assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
		assertEquals("Validation failed", body.getMessage());
		assertNotNull(body.getTimestamp());

		List<String> errors = body.getErrors();
		assertEquals(2, errors.size());
		assertTrue(errors.contains("field1: must not be null"));
		assertTrue(errors.contains("field2: must be positive"));
	}

	@Test
	void handleUnknownException_ShouldReturnInternalServerErrorResponse() {
		// Arrange
		String errorMessage = "Unexpected error occurred";
		Exception ex = new Exception(errorMessage);

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnknownException(ex);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals(errorMessage, Objects.requireNonNull(response.getBody()).getMessage());
		assertEquals(500, response.getBody().getStatus());
		assertNotNull(response.getBody().getTimestamp());
	}

	@Test
	void errorResponse_ShouldContainCurrentTimestamp() {
		// Arrange
		String errorMessage = "Test error";
		Exception ex = new Exception(errorMessage);
		LocalDateTime beforeTest = LocalDateTime.now();

		// Act
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnknownException(ex);
		LocalDateTime afterTest = LocalDateTime.now();

		// Assert
		LocalDateTime responseTimestamp = response.getBody().getTimestamp();
		assertTrue(responseTimestamp.isAfter(beforeTest) || responseTimestamp.isEqual(beforeTest));
		assertTrue(responseTimestamp.isBefore(afterTest) || responseTimestamp.isEqual(afterTest));
	}
}