package com.ak.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ak.dto.JobDto;
import com.ak.entity.Company;
import com.ak.entity.Job;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.JobRepository;
import com.ak.utils.JobMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class JobServiceImpl implements JobService {

	private JobRepository jobRepository;

	private CompanyService companyService;

	private UserService userService;

	private JobMapper jobMapper;

	private RedisService redisService;

	public JobServiceImpl(JobRepository jobRepository, CompanyService companyService, UserService userService,
			JobMapper jobMapper, RedisService redisService) {
		super();
		this.jobRepository = jobRepository;
		this.companyService = companyService;
		this.userService = userService;
		this.jobMapper = jobMapper;
		this.redisService = redisService;
	}

	@Override
	public Page<JobDto> getAllJobs(Pageable pageable) {
		String cacheKey = String.format("jobs:page:%d:size:%d:sort:%s", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		// Try cache first
		List<JobDto> cachedContent = redisService.getListValue(cacheKey + ":content",
				new TypeReference<List<JobDto>>() {
				});
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);

		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}

		// Fetch from DB
		Page<Job> page = jobRepository.findAll(pageable);
		List<JobDto> content = page.map(jobMapper::convertToDto).getContent();

		// Cache components separately
		redisService.setValue(cacheKey + ":content", content, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);

		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	@Override
	public JobDto getJobById(Long id) {
		Job job = this.findJobById(id);
		return jobMapper.convertToDto(job);
	}

	@Override
	public JobDto createJob(JobDto jobDto) {
		Users user = userService.findUserById(jobDto.getEmployerId());
		Company company = companyService.findCompanyById(jobDto.getCompanyId());
		Job job = jobMapper.convertToEntity(jobDto);
		job.setCompany(company);
		job.setPostedBy(user);
		Job savedJob = jobRepository.save(job);
		JobDto dto = jobMapper.convertToDto(savedJob);
		redisService.deleteKeysByPattern("jobs:page:*");
		redisService.setValue("job:" + dto.getId(), dto, 3600L);
		return dto;
	}

	@Override
	public JobDto updateJob(Long id, JobDto jobDto) {
		Job job = this.findJobById(id);
		Company company = companyService.findCompanyById(jobDto.getCompanyId());
		job.setTitle(jobDto.getTitle());
		job.setDescription(jobDto.getDescription());
		job.setCompany(company);
		job.setLocation(jobDto.getLocation());
		job.setSalary(jobDto.getSalary());
		job.setExpiryDate(jobDto.getExpiryDate());
		Job updatedJob = jobRepository.save(job);
		JobDto dto = jobMapper.convertToDto(updatedJob);
		redisService.deleteKey("job:" + updatedJob.getId());
		redisService.setValue("job:" + updatedJob.getId(), dto, 3600L);
		return dto;
	}

	@Override
	public void deleteJob(Long id) {
		jobRepository.deleteById(id);
		redisService.deleteKey("job:" + id);
	}

	@Override
	public Page<JobDto> searchJobs(String title, String location, Long companyId, Pageable pageable) {
		// Build cache key
		String cacheKey = String.format("jobs:search:title:%s:location:%s:company:%s:page:%d:size:%d:sort:%s",
				title != null ? title : "null", location != null ? location : "null",
				companyId != null ? companyId : "null", pageable.getPageNumber(), pageable.getPageSize(),
				pageable.getSort());

		List<JobDto> cachedContent = redisService.getListValue(cacheKey + ":content",
				new TypeReference<List<JobDto>>() {
				});
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);

		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}

		Page<Job> page;
		if (title != null && location != null && companyId != null) {
			page = jobRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCompanyId(title,
					location, companyId, pageable);
		} else if (title != null && location != null) {
			page = jobRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(title, location,
					pageable);
		} else if (title != null) {
			page = jobRepository.findByTitleContainingIgnoreCase(title, pageable);
		} else if (location != null) {
			page = jobRepository.findByLocationContainingIgnoreCase(location, pageable);
		} else if (companyId != null) {
			page = jobRepository.findByCompanyId(companyId, pageable);
		} else {
			page = jobRepository.findAll(pageable);
		}

		List<JobDto> content = page.map(jobMapper::convertToDto).getContent();

		// Cache results
		redisService.setValue(cacheKey + ":content", content, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);

		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	public Job findJobById(Long id) {
		String key = "job:" + id;
		Job cachedJob = redisService.getValue(key, Job.class);
		if (cachedJob != null) {
			return cachedJob;
		}
		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
		redisService.setValue(key, job, 3600L);
		return job;
	}

}