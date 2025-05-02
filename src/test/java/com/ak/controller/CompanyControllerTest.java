package com.ak.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import com.ak.dto.CompanyDto;
import com.ak.service.CompanyService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

	private MockMvc mockMvc;

	@Mock
	private CompanyService companyService;

	@InjectMocks
	private CompanyController companyController;

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		 mockMvc = MockMvcBuilders.standaloneSetup(companyController)
	                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
	                .build();
	}

	@Test
	void getAllCompanies_ShouldReturnPageOfCompanies() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		CompanyDto companyDto = new CompanyDto(1L, "Test Company", "Technology", "Test Industry", "http://test.com");
		Page<CompanyDto> page = new PageImpl<>(Collections.singletonList(companyDto), pageable, 1);

		when(companyService.getAllCompanies(any(Pageable.class))).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/companies").param("page", "0").param("size", "10").param("sort", "id,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].name").value("Test Company"))
				.andExpect(jsonPath("$.content[0].industry").value("Test Industry"))
				.andExpect(jsonPath("$.content[0].website").value("http://test.com"));
	}

	@Test
	void getCompanyById_ShouldReturnCompany() throws Exception {
		// Arrange
		CompanyDto companyDto = new CompanyDto(1L, "Test Company", "Technology", "industry", "website");
		when(companyService.getCompanyById(1L)).thenReturn(companyDto);

		// Act & Assert
		mockMvc.perform(get("/api/companies/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Test Company"))
				.andExpect(jsonPath("$.industry").value("industry"));
	}

	@Test
	void createCompany_ShouldReturnCreatedCompany() throws Exception {
		// Arrange
		CompanyDto inputDto = new CompanyDto(null, "New Company", "Finance", "industry", "website");
		CompanyDto outputDto = new CompanyDto(1L, "New Company", "Finance", "industry", "website");

		when(companyService.createCompany(any(CompanyDto.class))).thenReturn(outputDto);

		// Act & Assert
		mockMvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputDto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.name").value("New Company"))
				.andExpect(jsonPath("$.industry").value("industry"));
	}

	@Test
	void updateCompany_ShouldReturnUpdatedCompany() throws Exception {
		// Arrange
		CompanyDto inputDto = new CompanyDto(null, "Updated Company", "Healthcare", "industry", "website");
		CompanyDto outputDto = new CompanyDto(1L, "Updated Company", "Healthcare", "industry", "website");

		when(companyService.updateCompany(eq(1L), any(CompanyDto.class))).thenReturn(outputDto);

		// Act & Assert
		mockMvc.perform(put("/api/companies/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputDto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L)).andExpect(jsonPath("$.name").value("Updated Company"))
				.andExpect(jsonPath("$.industry").value("industry"));
	}

	@Test
	void deleteCompany_ShouldReturnSuccessMessage() throws Exception {
		// Arrange - No need to mock anything for void method unless it throws exception

		// Act & Assert
		mockMvc.perform(delete("/api/companies/1")).andExpect(status().isOk())
				.andExpect(content().string("Company deleted successfully"));
	}

	@Test
	void searchCompanies_ShouldReturnFilteredResults() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		CompanyDto companyDto = new CompanyDto(1L, "Tech Corp", "Technology", "industry", "website");
		Page<CompanyDto> page = new PageImpl<>(Collections.singletonList(companyDto), pageable, 1);

		when(companyService.searchCompanies("Tech Corp", "industry", pageable)).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/companies/search").param("name", "Tech Corp").param("industry", "industry")
				.param("page", "0").param("size", "10")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].name").value("Tech Corp"))
				.andExpect(jsonPath("$.content[0].industry").value("industry"));
	}

	@Test
	void searchCompanies_WithPartialParameters_ShouldReturnResults() throws Exception {
		// Arrange
		Pageable pageable = PageRequest.of(0, 10);
		CompanyDto companyDto = new CompanyDto(1L, "Tech Corp", "Technology", "industry", "website");
		Page<CompanyDto> page = new PageImpl<>(Collections.singletonList(companyDto), pageable, 1);

		when(companyService.searchCompanies("Tech Corp", null, pageable)).thenReturn(page);

		// Act & Assert
		mockMvc.perform(get("/api/companies/search").param("name", "Tech Corp").param("page", "0").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1L))
				.andExpect(jsonPath("$.content[0].name").value("Tech Corp"));
	}
}