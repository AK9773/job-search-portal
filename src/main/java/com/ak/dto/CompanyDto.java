package com.ak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {

	private Long id;
	private String name;
	private String description;
	private String industry;
	private String website;

}
