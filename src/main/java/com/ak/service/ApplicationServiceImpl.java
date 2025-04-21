package com.ak.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ak.dto.ApplicationDto;
import com.ak.entity.Application;
import com.ak.entity.Job;
import com.ak.entity.Status;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.ApplicationRepository;
import com.ak.utils.ApplicationMapper;

@Service
public class ApplicationServiceImpl implements ApplicationService {

	private ApplicationRepository applicationRepository;

	private ApplicationMapper applicationMapper;

	private JobService jobService;

	private UserService userService;

	private RedisService redisService;

	public ApplicationServiceImpl(ApplicationRepository applicationRepository, ApplicationMapper applicationMapper,
			JobService jobService, UserService userService, RedisService redisService) {
		this.applicationRepository = applicationRepository;
		this.applicationMapper = applicationMapper;
		this.jobService = jobService;
		this.userService = userService;
		this.redisService = redisService;
	}

	@Override
	public Page<ApplicationDto> getAllApplications(Pageable pageable) {
		String cacheKey = String.format("application:page:%d:size:%d:sort:%s", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());
		List<ApplicationDto> cachedContent = redisService.getValue(cacheKey + ":content", List.class);
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);
		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}

		Page<Application> page = applicationRepository.findAll(pageable);
		List<ApplicationDto> content = page.map(applicationMapper::convertToDto).getContent();

		redisService.setValue(cacheKey + ":content", content, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);
		return new PageImpl<>(content, pageable, page.getTotalElements());

	}

	@Override
	public ApplicationDto getApplicationById(Long id) {

		Application application = this.findApplicationById(id);
		return applicationMapper.convertToDto(application);

	}

	@Override
	public ApplicationDto createApplication(ApplicationDto applicationDto) {
		Job job = jobService.findJobById(applicationDto.getJobId());
		Users user = userService.findUserById(applicationDto.getSeekerId());
		Application application = applicationMapper.convertToEntity(applicationDto);
		application.setJob(job);
		application.setUser(user);
		Application savedApplication = applicationRepository.save(application);
		ApplicationDto applicationDto2 = applicationMapper.convertToDto(savedApplication);
		redisService.deleteKeysByPattern("*application:page:*");
		redisService.setValue("application:" + savedApplication.getId(), applicationDto2, 3600L);
		return applicationDto2;
	}

	@Override
	public ApplicationDto updateStatus(Long id, Status status) {
		Application application = this.findApplicationById(id);

		application.setStatus(status);
		Application updatedApplication = applicationRepository.save(application);
		ApplicationDto applicationDto = applicationMapper.convertToDto(updatedApplication);
		redisService.deleteKeysByPattern("*application:page:*");
		redisService.deleteKey("application:" + updatedApplication.getId());
		redisService.setValue("application:" + updatedApplication.getId(), applicationDto, 3600L);
		return applicationDto;
	}

	@Override
	public Page<ApplicationDto> getApplicationsByJob(Long jobId, Pageable pageable) {
		Page<Application> page = applicationRepository.findByJobId(jobId, pageable);
		return page.map(applicationMapper::convertToDto);
	}

	@Override
	public Page<ApplicationDto> getApplicationsByUser(Long userId, Pageable pageable) {
		String cacheKey = String.format("userId:%d:application:page:%d:size:%d:sort:%s", userId,
				pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
		List<ApplicationDto> cachedContent = redisService.getValue(cacheKey + ":content", List.class);
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);
		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}
		Page<Application> page = applicationRepository.findByUserId(userId, pageable);
		List<ApplicationDto> content = page.map(applicationMapper::convertToDto).getContent();
		redisService.setValue(cacheKey + ":content", content, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	@Override
	public Application findApplicationById(Long id) {
		String cacheKey = "application:" + id;
		Application cachedContent = redisService.getValue(cacheKey, Application.class);
		if (cachedContent != null) {
			return cachedContent;
		}
		Application application = applicationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
		redisService.setValue(cacheKey, application, 3600L);
		return application;
	}
}