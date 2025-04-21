package com.ak.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.JobDto;
import com.ak.service.JobService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private JobService jobService;

	public JobController(JobService jobService) {
		super();
		this.jobService = jobService;
	}

	@GetMapping
	public ResponseEntity<Page<JobDto>> getAllJobs(Pageable pageable) {
		return ResponseEntity.ok(jobService.getAllJobs(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<JobDto> getJobById(@PathVariable Long id) {
		return ResponseEntity.ok(jobService.getJobById(id));
	}

	@PostMapping
	public ResponseEntity<JobDto> createJob(@RequestBody JobDto jobDto) {
		return ResponseEntity.ok(jobService.createJob(jobDto));
	}

	@PutMapping("/{id}")
	public ResponseEntity<JobDto> updateJob(@PathVariable Long id, @RequestBody JobDto jobDto) {
		return ResponseEntity.ok(jobService.updateJob(id, jobDto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteJob(@PathVariable Long id) {
		jobService.deleteJob(id);
		return ResponseEntity.ok("Job deleted with id: " + id);
	}

	@GetMapping("/search")
	public ResponseEntity<Page<JobDto>> searchJobs(@RequestParam(required = false) String title,
			@RequestParam(required = false) String location, @RequestParam(required = false) Long companyId,
			Pageable pageable) {
		return ResponseEntity.ok(jobService.searchJobs(title, location, companyId, pageable));
	}
}