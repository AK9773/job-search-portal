package com.ak.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ak.dto.ApplicationDto;
import com.ak.entity.Application;
import com.ak.entity.Status;

public interface ApplicationService {
	Page<ApplicationDto> getAllApplications(Pageable pageable);

	ApplicationDto getApplicationById(Long id);

	ApplicationDto createApplication(ApplicationDto applicationDto);

	ApplicationDto updateStatus(Long id, Status status);

	Page<ApplicationDto> getApplicationsByJob(Long jobId, Pageable pageable);

	Page<ApplicationDto> getApplicationsByUser(Long userId, Pageable pageable);
	
	Application findApplicationById(Long id);
}