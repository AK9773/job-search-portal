package com.ak.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ak.dto.ApplicationDto;
import com.ak.entity.Application;
import com.ak.entity.Job;
import com.ak.entity.Status;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.ApplicationRepository;
import com.ak.utils.ApplicationMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

	@Mock
	private ApplicationRepository applicationRepository;

	@Mock
	private ApplicationMapper applicationMapper;

	@Mock
	private JobService jobService;

	@Mock
	private UserService userService;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private ApplicationServiceImpl applicationService;

	private Application application;
	private ApplicationDto applicationDto;
	private Job job;
	private Users user;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		pageable = PageRequest.of(0, 10);

		job = new Job();
		job.setId(1L);

		user = new Users();
		user.setId(1L);

		application = new Application();
		application.setId(1L);
		application.setJob(job);
		application.setUser(user);
		application.setStatus(Status.APPLIED);

		applicationDto = new ApplicationDto();
		applicationDto.setId(1L);
		applicationDto.setJobId(1L);
		applicationDto.setSeekerId(1L);
		applicationDto.setStatus("APPLIED");
	}

	@Test
	void getAllApplications_ShouldReturnPageFromCache() {
		// Arrange
		String cacheKey = "application:page:0:size:10:sort:UNSORTED";
		List<ApplicationDto> cachedContent = Arrays.asList(applicationDto);
		Long totalElements = 1L;

		when(redisService.getListValue(eq("application:page:0:size:10:sort:UNSORTED:content"),
				ArgumentMatchers.<TypeReference<List<ApplicationDto>>>any())).thenReturn(cachedContent);
		when(redisService.getValue(eq(cacheKey + ":total"), eq(Long.class))).thenReturn(totalElements);

		// Act
		Page<ApplicationDto> result = applicationService.getAllApplications(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		assertEquals(applicationDto, result.getContent().get(0));
		verify(redisService, times(1)).getListValue(eq(cacheKey + ":content"),
				ArgumentMatchers.<TypeReference<List<ApplicationDto>>>any());
		verify(redisService, times(1)).getValue(eq(cacheKey + ":total"), eq(Long.class));
		verifyNoInteractions(applicationRepository);
	}

	@Test
	void getAllApplications_ShouldFetchFromDBAndCache() {
		// Arrange
		String cacheKey = "application:page:0:size:10:sort:UNSORTED";
		List<Application> applications = Arrays.asList(application);
		Page<Application> page = new PageImpl<>(applications, pageable, 1);

		when(redisService.getListValue(eq(cacheKey + ":content"),
				ArgumentMatchers.<TypeReference<List<ApplicationDto>>>any())).thenReturn(null);
		when(redisService.getValue(eq(cacheKey + ":total"), eq(Long.class))).thenReturn(null);
		when(applicationRepository.findAll(pageable)).thenReturn(page);
		when(applicationMapper.convertToDto(application)).thenReturn(applicationDto);

		// Act
		Page<ApplicationDto> result = applicationService.getAllApplications(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		assertEquals(applicationDto, result.getContent().get(0));

		verify(redisService).setValue(eq(cacheKey + ":content"), eq(Arrays.asList(applicationDto)), eq(3600L));
		verify(redisService).setValue(eq(cacheKey + ":total"), eq(1L), eq(3600L));
	}

	@Test
	void getApplicationById_ShouldReturnApplicationDto() {
		// Arrange
		when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
		when(applicationMapper.convertToDto(application)).thenReturn(applicationDto);

		// Act
		ApplicationDto result = applicationService.getApplicationById(1L);

		// Assert
		assertEquals(applicationDto, result);
		verify(applicationRepository).findById(1L);
		verify(applicationMapper).convertToDto(application);
	}

	@Test
	void getApplicationById_ShouldThrowExceptionWhenNotFound() {
		// Arrange
		when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			applicationService.getApplicationById(1L);
		});
	}

	@Test
	void createApplication_ShouldSaveAndCacheApplication() {
		// Arrange
		when(jobService.findJobById(1L)).thenReturn(job);
		when(userService.findUserById(1L)).thenReturn(user);
		when(applicationMapper.convertToEntity(applicationDto)).thenReturn(application);
		when(applicationRepository.save(application)).thenReturn(application);
		when(applicationMapper.convertToDto(application)).thenReturn(applicationDto);

		// Act
		ApplicationDto result = applicationService.createApplication(applicationDto);

		// Assert
		assertEquals(applicationDto, result);
		verify(applicationRepository).save(application);
		verify(redisService).deleteKeysByPattern("*application:page:*");
		verify(redisService).setValue(eq("application:" + application.getId()), eq(applicationDto), eq(3600L));
	}

	@Test
	void updateStatus_ShouldUpdateAndCacheApplication() {
		// Arrange
		Status newStatus = Status.INTERVIEW;
		when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
		when(applicationRepository.save(application)).thenReturn(application);
		when(applicationMapper.convertToDto(application)).thenReturn(applicationDto);

		// Act
		ApplicationDto result = applicationService.updateStatus(1L, newStatus);

		// Assert
		assertEquals(Status.INTERVIEW, application.getStatus());
		assertEquals(applicationDto, result);
		verify(redisService).deleteKeysByPattern("*application:page:*");
		verify(redisService).deleteKey("application:1");
		verify(redisService).setValue(eq("application:1"), eq(applicationDto), eq(3600L));
	}

	@Test
	void getApplicationsByJob_ShouldReturnPageOfApplications() {
		// Arrange
		List<Application> applications = Arrays.asList(application);
		Page<Application> page = new PageImpl<>(applications, pageable, 1);

		when(applicationRepository.findByJobId(1L, pageable)).thenReturn(page);
		when(applicationMapper.convertToDto(application)).thenReturn(applicationDto);

		// Act
		Page<ApplicationDto> result = applicationService.getApplicationsByJob(1L, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		assertEquals(applicationDto, result.getContent().get(0));
	}

	@Test
	void getApplicationsByUser_ShouldReturnPageFromCache() {
		// Arrange
		String cacheKey = "userId:1:application:page:0:size:10:sort:UNSORTED";
		List<ApplicationDto> cachedContent = Arrays.asList(applicationDto);
		Long totalElements = 1L;

		when(redisService.getListValue(eq(cacheKey + ":content"),
				ArgumentMatchers.<TypeReference<List<ApplicationDto>>>any())).thenReturn(cachedContent);
		when(redisService.getValue(eq(cacheKey + ":total"), eq(Long.class))).thenReturn(totalElements);

		// Act
		Page<ApplicationDto> result = applicationService.getApplicationsByUser(1L, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		assertEquals(applicationDto, result.getContent().get(0));

		verifyNoInteractions(applicationRepository);
	}

	@Test
	void findApplicationById_ShouldReturnApplicationFromCache() {
		// Arrange
		String cacheKey = "application:1";
		when(redisService.getValue(cacheKey, Application.class)).thenReturn(application);

		// Act
		Application result = applicationService.findApplicationById(1L);

		// Assert
		assertEquals(application, result);
		verifyNoInteractions(applicationRepository);
	}

	@Test
	void findApplicationById_ShouldFetchFromDBAndCacheWhenNotInCache() {
		// Arrange
		String cacheKey = "application:1";
		when(redisService.getValue(cacheKey, Application.class)).thenReturn(null);
		when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

		// Act
		Application result = applicationService.findApplicationById(1L);

		// Assert
		assertEquals(application, result);
		verify(redisService).setValue(eq(cacheKey), eq(application), eq(3600L));
	}

	@Test
	void findApplicationById_ShouldThrowExceptionWhenNotFound() {
		// Arrange
		when(redisService.getValue("application:1", Application.class)).thenReturn(null);
		when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			applicationService.findApplicationById(1L);
		});
	}
}