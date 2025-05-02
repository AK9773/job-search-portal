package com.ak.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ak.dto.JobDto;
import com.ak.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

	private MockMvc mockMvc;

	@Mock
	private JobService jobService;

	@InjectMocks
	private JobController jobController;

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		
		objectMapper.registerModule(new JavaTimeModule()); // Add Java 8 date/time support
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mockMvc = MockMvcBuilders.standaloneSetup(jobController)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
	}

	@Test
	void getAllJobs_ShouldReturnPageOfJobs() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		JobDto jobDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		Page<JobDto> page = new PageImpl<>(Collections.singletonList(jobDto), pageable, 1);

		when(jobService.getAllJobs(any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/jobs").param("page", "0").param("size", "10").param("sort", "id,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].title").value("Software Engineer"))
				.andExpect(jsonPath("$.content[0].location").value("New York"));
	}

	@Test
	void getJobById_ShouldReturnJob() throws Exception {
		// Arrange
		JobDto jobDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		when(jobService.getJobById(1L)).thenReturn(jobDto);

		// Act & Assert
		mockMvc.perform(get("/api/jobs/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.title").value("Software Engineer"))
				.andExpect(jsonPath("$.location").value("New York"));
	}

	@Test
	void createJob_ShouldReturnCreatedJob() throws Exception {
		// Arrange
		JobDto inputDto = new JobDto(null, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		JobDto outputDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);

		when(jobService.createJob(any(JobDto.class))).thenReturn(outputDto);

		// Act & Assert
		mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputDto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.title").value("Software Engineer"))
				.andExpect(jsonPath("$.location").value("New York"));
	}

	@Test
	void updateJob_ShouldReturnUpdatedJob() throws Exception {
		// Arrange
		JobDto inputDto = new JobDto(null, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		JobDto outputDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		when(jobService.updateJob(eq(1L), any(JobDto.class))).thenReturn(outputDto);

		// Act & Assert
		mockMvc.perform(put("/api/jobs/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputDto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.title").value("Software Engineer"))
				.andExpect(jsonPath("$.location").value("New York"));
	}

	@Test
	void deleteJob_ShouldReturnSuccessMessage() throws Exception {
		// Act & Assert
		mockMvc.perform(delete("/api/jobs/1")).andExpect(status().isOk())
				.andExpect(content().string("Job deleted with id: 1"));

		verify(jobService).deleteJob(1L);
	}

	@Test
	void searchJobs_ShouldReturnFilteredResults() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		JobDto jobDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		Page<JobDto> page = new PageImpl<>(Collections.singletonList(jobDto), pageable, 1);

		when(jobService.searchJobs(eq("Java"), eq("Boston"), eq(3L), any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/jobs/search").param("title", "Java").param("location", "Boston")
				.param("companyId", "3").param("page", "0").param("size", "10")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].title").value("Software Engineer"))
				.andExpect(jsonPath("$.content[0].location").value("New York"))
				.andExpect(jsonPath("$.content[0].companyId").value(1L));
	}

	@Test
	void searchJobs_WithPartialParameters_ShouldReturnResults() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		JobDto jobDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		Page<JobDto> page = new PageImpl<>(Collections.singletonList(jobDto), pageable, 1);

		when(jobService.searchJobs(eq("Software Engineer"), any(), any(), any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(
				get("/api/jobs/search").param("title", "Software Engineer").param("page", "0").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].title").value("Software Engineer"))
				.andExpect(jsonPath("$.content[0].location").value("New York"));
	}

	@Test
	void searchJobs_WithNullParameters_ShouldReturnResults() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		JobDto jobDto = new JobDto(1L, "Software Engineer", "Develop software", "New York", 100000.0,
				LocalDateTime.now(), LocalDate.of(2025, 5, 5), 1L, 1L);
		Page<JobDto> page = new PageImpl<>(Collections.singletonList(jobDto), pageable, 1);

		when(jobService.searchJobs(any(), any(), any(), any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/jobs/search").param("page", "0").param("size", "10")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].title").value("Software Engineer"));
	}
}