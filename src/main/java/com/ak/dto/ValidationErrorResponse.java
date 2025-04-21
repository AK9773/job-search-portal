package com.ak.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ValidationErrorResponse {

	private int status;
	private String message;
	private LocalDateTime timestamp;
	private List<String> errors;
	public ValidationErrorResponse(int status, String message, LocalDateTime timestamp, List<String> errors) {
		super();
		this.status = status;
		this.message = message;
		this.timestamp = timestamp;
		this.errors = errors;
	}
	public ValidationErrorResponse(int status, String message, LocalDateTime timestamp) {
		super();
		this.status = status;
		this.message = message;
		this.timestamp = timestamp;
	}
	
	
}
