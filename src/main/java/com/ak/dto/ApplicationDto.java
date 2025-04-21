package com.ak.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {

	private Long id;
	@Pattern(regexp = "APPLIED|INTERVIEW|REJECTED", message = "Invalid Status")
	private String status;
	private LocalDateTime appliedDate;
	@NotNull(message = "Can not be null")
	private Long seekerId;
	@NotNull
	private Long jobId;

}
