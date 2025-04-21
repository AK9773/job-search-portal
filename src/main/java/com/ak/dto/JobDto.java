package com.ak.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobDto {

	private Long id;
	private String title;
	private String description;
	private String location;
	private Double salary;
	private LocalDateTime postedDate;
	private LocalDate expiryDate;
	private long employerId;
	private long companyId;

}
