package com.ak.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.ApplicationDto;
import com.ak.entity.Status;
import com.ak.service.ApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

	private ApplicationService applicationService;

	public ApplicationController(ApplicationService applicationService) {
		super();
		this.applicationService = applicationService;
	}

	@GetMapping
	public ResponseEntity<Page<ApplicationDto>> getAllApplications(Pageable pageable) {
		return ResponseEntity.ok(applicationService.getAllApplications(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApplicationDto> getApplicationById(@PathVariable Long id) {
		return ResponseEntity.ok(applicationService.getApplicationById(id));
	}

	@PostMapping
	public ResponseEntity<ApplicationDto> createApplication(@Valid @RequestBody ApplicationDto applicationDto) {
		return new ResponseEntity<ApplicationDto>(applicationService.createApplication(applicationDto),
				HttpStatus.CREATED);
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApplicationDto> updateApplicationStatus(@PathVariable Long id, @RequestParam Status status) {
		return ResponseEntity.ok(applicationService.updateStatus(id, status));
	}

	@GetMapping("/job/{jobId}")
	public ResponseEntity<Page<ApplicationDto>> getApplicationsByJob(@PathVariable Long jobId, Pageable pageable) {
		return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, pageable));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<Page<ApplicationDto>> getApplicationsByUser(@PathVariable Long userId, Pageable pageable) {
		return ResponseEntity.ok(applicationService.getApplicationsByUser(userId, pageable));
	}
}