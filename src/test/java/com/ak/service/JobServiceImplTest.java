package com.ak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
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

import com.ak.dto.JobDto;
import com.ak.entity.Company;
import com.ak.entity.Job;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.JobRepository;
import com.ak.utils.JobMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private CompanyService companyService;

	@Mock
	private UserService userService;

	@Mock
	private JobMapper jobMapper;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private JobServiceImpl jobService;

	private Job job;
	private JobDto jobDto;
	private Company company;
	private Users user;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		pageable = PageRequest.of(0, 10);

		company = new Company();
		company.setId(1L);
		company.setName("Test Company");

		user = new Users();
		user.setId(1L);
		user.setName("Test User");

		job = new Job();
		job.setId(1L);
		job.setTitle("Test Job");
		job.setCompany(company);
		job.setPostedBy(user);

		jobDto = new JobDto();
		jobDto.setId(1L);
		jobDto.setTitle("Test Job");
		jobDto.setCompanyId(1L);
		jobDto.setEmployerId(1L);
	}

	@Test
	void getJobById_WhenExists_ReturnsJobDto() {
		// Arrange
		when(redisService.getValue("job:1", Job.class)).thenReturn(null);
		when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).setValue(eq("job:1"), eq(job), anyLong());

		// Act
		JobDto result = jobService.getJobById(1L);

		// Assert
		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("Test Job", result.getTitle());
		verify(redisService).getValue("job:1", Job.class);
		verify(jobRepository).findById(1L);
		verify(redisService).setValue(eq("job:1"), eq(job), eq(3600L));
	}

	@Test
	void getJobById_WhenCached_ReturnsFromCache() {
		// Arrange
		when(redisService.getValue("job:1", Job.class)).thenReturn(job);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);

		// Act
		JobDto result = jobService.getJobById(1L);

		// Assert
		assertNotNull(result);
		verify(redisService).getValue("job:1", Job.class);
		verifyNoInteractions(jobRepository);
	}

	@Test
	void createJob_SavesAndReturnsDto() {
		// Arrange
		when(userService.findUserById(1L)).thenReturn(user);
		when(companyService.findCompanyById(1L)).thenReturn(company);
		when(jobMapper.convertToEntity(jobDto)).thenReturn(job);
		when(jobRepository.save(job)).thenReturn(job);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).deleteKeysByPattern("jobs:page:*");
		doNothing().when(redisService).setValue(eq("job:1"), eq(jobDto), anyLong());

		// Act
		JobDto result = jobService.createJob(jobDto);

		// Assert
		assertNotNull(result);
		assertEquals(1L, result.getId());
		verify(jobRepository).save(job);
		verify(redisService).deleteKeysByPattern("jobs:page:*");
		verify(redisService).setValue(eq("job:1"), eq(jobDto), eq(3600L));
	}

	@Test
	void updateJob_WhenExists_UpdatesAndReturnsDto() {
		// Arrange
		JobDto updatedDto = new JobDto();
		updatedDto.setTitle("Updated Job");
		updatedDto.setCompanyId(1L);

		when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
		when(companyService.findCompanyById(1L)).thenReturn(company);
		when(jobRepository.save(any())).thenReturn(job);
		when(jobMapper.convertToDto(job)).thenReturn(updatedDto);
		doNothing().when(redisService).deleteKey("job:1");
		doNothing().when(redisService).setValue(eq("job:1"), any(), anyLong());

		// Act
		JobDto result = jobService.updateJob(1L, updatedDto);

		// Assert
		assertEquals("Updated Job", result.getTitle());
		verify(jobRepository).save(job);
		verify(redisService).deleteKey("job:1");
	}

	@Test
	void deleteJob_DeletesFromRepositoryAndCache() {
		// Arrange
		doNothing().when(jobRepository).deleteById(1L);
		doNothing().when(redisService).deleteKey("job:1");

		// Act
		jobService.deleteJob(1L);

		// Assert
		verify(jobRepository).deleteById(1L);
		verify(redisService).deleteKey("job:1");
	}

	@Test
	void getAllJobs_ReturnsPageFromCache() {
		// Arrange
		List<JobDto> cachedContent = Collections.singletonList(jobDto);
		when(redisService.getListValue(eq("jobs:page:0:size:10:sort:UNSORTED:content"),
				ArgumentMatchers.<TypeReference<List<JobDto>>>any())).thenReturn(cachedContent);

		when(redisService.getValue(eq("jobs:page:0:size:10:sort:UNSORTED:total"), eq(Long.class))).thenReturn(1L);

		// Act
		Page<JobDto> result = jobService.getAllJobs(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		verify(redisService).getListValue(anyString(), any());
		verify(redisService).getValue(anyString(), eq(Long.class));
		verifyNoInteractions(jobRepository);
	}

	@Test
	void getAllJobs_ReturnsPageFromDatabaseWhenCacheEmpty() {
		// Arrange
		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Job> jobPage = new PageImpl<>(Collections.singletonList(job), pageable, 1);
		when(jobRepository.findAll(pageable)).thenReturn(jobPage);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<JobDto> result = jobService.getAllJobs(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(jobRepository).findAll(pageable);
		verify(redisService).setValue(contains(":content"), anyList(), eq(3600L));
		verify(redisService).setValue(contains(":total"), eq(1L), eq(3600L));
	}

	@Test
	void searchJobs_ByTitleAndLocationAndCompany_ReturnsFilteredPage() {
		// Arrange
		String title = "Test";
		String location = "City";
		Long companyId = 1L;

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Job> jobPage = new PageImpl<>(Collections.singletonList(job), pageable, 1);
		when(jobRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCompanyId(title, location,
				companyId, pageable)).thenReturn(jobPage);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<JobDto> result = jobService.searchJobs(title, location, companyId, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(jobRepository).findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCompanyId(title,
				location, companyId, pageable);
	}

	@Test
	void searchJobs_ByTitleOnly_ReturnsFilteredPage() {
		// Arrange
		String title = "Test";

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Job> jobPage = new PageImpl<>(Collections.singletonList(job), pageable, 1);
		when(jobRepository.findByTitleContainingIgnoreCase(title, pageable)).thenReturn(jobPage);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<JobDto> result = jobService.searchJobs(title, null, null, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(jobRepository).findByTitleContainingIgnoreCase(title, pageable);
	}

	@Test
	void searchJobs_ByCompanyOnly_ReturnsFilteredPage() {
		// Arrange
		Long companyId = 1L;

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Job> jobPage = new PageImpl<>(Collections.singletonList(job), pageable, 1);
		when(jobRepository.findByCompanyId(companyId, pageable)).thenReturn(jobPage);
		when(jobMapper.convertToDto(job)).thenReturn(jobDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<JobDto> result = jobService.searchJobs(null, null, companyId, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(jobRepository).findByCompanyId(companyId, pageable);
	}

	@Test
	void getJobById_WhenNotExists_ThrowsException() {
		// Arrange
		when(redisService.getValue("job:1", Job.class)).thenReturn(null);
		when(jobRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			jobService.getJobById(1L);
		});
	}

	@Test
	void createJob_WhenUserNotExists_ThrowsException() {
		// Arrange
		when(userService.findUserById(1L)).thenThrow(new ResourceNotFoundException("User not found"));

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			jobService.createJob(jobDto);
		});
	}

	@Test
	void updateJob_WhenNotExists_ThrowsException() {
		// Arrange
		when(jobRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			jobService.updateJob(1L, jobDto);
		});
	}

}