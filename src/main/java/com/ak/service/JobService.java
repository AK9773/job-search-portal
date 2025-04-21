package com.ak.service;

import com.ak.dto.JobDto;
import com.ak.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {
	Page<JobDto> getAllJobs(Pageable pageable);

	JobDto getJobById(Long id);

	JobDto createJob(JobDto jobDto);

	JobDto updateJob(Long id, JobDto jobDto);

	void deleteJob(Long id);

	Page<JobDto> searchJobs(String title, String location, Long companyId, Pageable pageable);

	Job findJobById(Long id);
}