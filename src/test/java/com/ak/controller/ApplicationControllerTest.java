package com.ak.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.ak.dto.ApplicationDto;
import com.ak.entity.Status;
import com.ak.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

	@Mock
	private ApplicationService applicationService;

	@InjectMocks
	private ApplicationController applicationController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	void getAllApplications_ShouldReturnPageOfApplications() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		ApplicationDto applicationDto = createSampleApplicationDto();
		Page<ApplicationDto> page = new PageImpl<>(Collections.singletonList(applicationDto), pageable, 1);

		when(applicationService.getAllApplications(any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/applications").param("page", "0").param("size", "10")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(applicationDto.getId()));
	}

	@Test
	void getApplicationById_ShouldReturnApplication() throws Exception {
		// Arrange
		Long id = 1L;
		ApplicationDto applicationDto = createSampleApplicationDto();

		when(applicationService.getApplicationById(id)).thenReturn(applicationDto);

		// Act & Assert
		mockMvc.perform(get("/api/applications/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(applicationDto.getId()));
	}

	@Test
	void createApplication_ShouldReturnCreatedApplication() throws Exception {
		// Arrange
		ApplicationDto inputDto = createSampleApplicationDto();
		inputDto.setId(null); // ID should be null for creation
		ApplicationDto savedDto = createSampleApplicationDto();

		when(applicationService.createApplication(any(ApplicationDto.class))).thenReturn(savedDto);

		// Act & Assert
		mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputDto))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(savedDto.getId()));
	}

	@Test
	void updateApplicationStatus_ShouldReturnUpdatedApplication() throws Exception {
		// Arrange
		Long id = 1L;
		Status newStatus = Status.INTERVIEW;
		ApplicationDto updatedDto = createSampleApplicationDto();
		updatedDto.setStatus(newStatus.toString());

		when(applicationService.updateStatus(eq(id), eq(newStatus))).thenReturn(updatedDto);

		// Act & Assert
		mockMvc.perform(put("/api/applications/{id}/status", id).param("status", newStatus.name()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value(newStatus.name()));
	}

	@Test
	void getApplicationsByJob_ShouldReturnPageOfApplications() throws Exception {
		// Arrange
		Long jobId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		ApplicationDto applicationDto = createSampleApplicationDto();
		Page<ApplicationDto> page = new PageImpl<>(Collections.singletonList(applicationDto), pageable, 1);

		when(applicationService.getApplicationsByJob(eq(jobId), any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/applications/job/{jobId}", jobId).param("page", "0").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].jobId").value(applicationDto.getJobId()));
	}

	@Test
	void getApplicationsByUser_ShouldReturnPageOfApplications() throws Exception {
		// Arrange
		Long userId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		ApplicationDto applicationDto = createSampleApplicationDto();
		Page<ApplicationDto> page = new PageImpl<>(Collections.singletonList(applicationDto), pageable, 1);

		when(applicationService.getApplicationsByUser(eq(userId), any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/applications/user/{userId}", userId).param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].seekerId").value(applicationDto.getSeekerId()));
	}

	private ApplicationDto createSampleApplicationDto() {
		ApplicationDto dto = new ApplicationDto();
		dto.setId(1L);
		dto.setJobId(101L);
		dto.setSeekerId(201L);
		dto.setStatus(Status.APPLIED.toString());
		return dto;
	}
}