package com.ak.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
	
	private int status;
	private String message;
	private LocalDateTime timestamp;
	private T data;

}
