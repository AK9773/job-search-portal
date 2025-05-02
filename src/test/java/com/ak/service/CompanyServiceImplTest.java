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

import com.ak.dto.CompanyDto;
import com.ak.entity.Company;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.CompanyRepository;
import com.ak.utils.CompanyMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private CompanyMapper companyMapper;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private CompanyServiceImpl companyService;

	private Company company;
	private CompanyDto companyDto;
	private Pageable pageable;

	@BeforeEach
	void setUp() {
		company = new Company();
		company.setId(1L);
		company.setName("Test Company");
		company.setDescription("Test Description");

		companyDto = new CompanyDto();
		companyDto.setId(1L);
		companyDto.setName("Test Company");
		companyDto.setDescription("Test Description");

		pageable = PageRequest.of(0, 10);
	}

	@Test
	void getCompanyById_WhenExists_ReturnsCompanyDto() {
		// Arrange
		when(redisService.getValue(eq("company:1"), eq(Company.class))).thenReturn(null);
		when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).setValue(eq("company:1"), eq(company), anyLong());
		// Act
		CompanyDto result = companyService.getCompanyById(1L);

		// Assert
		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("Test Company", result.getName());
		verify(redisService).getValue("company:1", Company.class);
		verify(companyRepository).findById(1L);
		verify(redisService).setValue(eq("company:1"), eq(company), eq(3600L));
	}

	@Test
	void getCompanyById_WhenNotExists_ThrowsException() {
		// Arrange
		when(redisService.getValue(eq("company:1"), eq(Company.class))).thenReturn(null);
		when(companyRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			companyService.getCompanyById(1L);
		});
	}

	@Test
	void createCompany_SavesAndReturnsDto() {
		// Arrange
		when(companyMapper.convertToEntity(companyDto)).thenReturn(company);
		when(companyRepository.save(company)).thenReturn(company);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).setValue(eq("company:" + company.getId()), eq(companyDto), anyLong());
		doNothing().when(redisService).deleteKeysByPattern("*company:page:*");

		// Act
		CompanyDto result = companyService.createCompany(companyDto);

		// Assert
		assertNotNull(result);
		assertEquals(1L, result.getId());
		verify(companyRepository).save(company);
		verify(redisService).deleteKeysByPattern("*company:page:*");
		verify(redisService).setValue(eq("company:1"), eq(companyDto), eq(3600L));
	}

	@Test
	void updateCompany_WhenExists_UpdatesAndReturnsDto() {
		// Arrange
		CompanyDto updatedDto = new CompanyDto();
		updatedDto.setName("Updated Name");

		when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
		when(companyRepository.save(any())).thenReturn(company);
		when(companyMapper.convertToDto(company)).thenReturn(updatedDto);
		doNothing().when(redisService).deleteKeysByPattern("*company:page:*");
		doNothing().when(redisService).deleteKey("company:1");
		doNothing().when(redisService).setValue(eq("company:1"), any(), anyLong());

		// Act
		CompanyDto result = companyService.updateCompany(1L, updatedDto);

		// Assert
		assertEquals("Updated Name", result.getName());
		verify(companyRepository).save(company);
		verify(redisService).deleteKeysByPattern("*company:page:*");
		verify(redisService).deleteKey("company:1");
	}

	@Test
	void deleteCompany_DeletesFromRepositoryAndCache() {
		// Arrange
		doNothing().when(redisService).deleteKey("company:1");
		doNothing().when(companyRepository).deleteById(1L);

		// Act
		companyService.deleteCompany(1L);

		// Assert
		verify(companyRepository).deleteById(1L);
		verify(redisService).deleteKey("company:1");
	}

	@Test
	void getAllCompanies_ReturnsPageFromCache() {
		// Arrange
		List<CompanyDto> cachedContent = Collections.singletonList(companyDto);
		when(redisService.getListValue(eq("company:page:0:size:10:sort:UNSORTED:content"),
				ArgumentMatchers.<TypeReference<List<CompanyDto>>>any())).thenReturn(cachedContent);
		when(redisService.getValue(eq("company:page:0:size:10:sort:UNSORTED:total"), eq(Long.class))).thenReturn(1L);

		// Act
		Page<CompanyDto> result = companyService.getAllCompanies(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		assertEquals(1, result.getContent().size());
		verify(redisService).getListValue(anyString(), any());
		verify(redisService).getValue(anyString(), eq(Long.class));
		verifyNoInteractions(companyRepository);
	}

	@Test
	void getAllCompanies_ReturnsPageFromDatabaseWhenCacheEmpty() {
		// Arrange
		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Company> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);
		when(companyRepository.findAll(pageable)).thenReturn(companyPage);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());
		// Act
		Page<CompanyDto> result = companyService.getAllCompanies(pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(companyRepository).findAll(pageable);
		verify(redisService).setValue(contains(":content"), anyList(), eq(3600L));
		verify(redisService).setValue(contains(":total"), eq(1L), eq(3600L));
	}

	@Test
	void searchCompanies_ByNameAndIndustry_ReturnsFilteredPage() {
		// Arrange
		String name = "Test";
		String industry = "Tech";

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Company> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);
		when(companyRepository.findByNameContainingAndIndustryContaining(name, industry, pageable))
				.thenReturn(companyPage);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);

		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<CompanyDto> result = companyService.searchCompanies(name, industry, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(companyRepository).findByNameContainingAndIndustryContaining(name, industry, pageable);
	}

	@Test
	void searchCompanies_ByNameOnly_ReturnsFilteredPage() {
		// Arrange
		String name = "Test";

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Company> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);
		when(companyRepository.findByNameContaining(name, pageable)).thenReturn(companyPage);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<CompanyDto> result = companyService.searchCompanies(name, null, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(companyRepository).findByNameContaining(name, pageable);
	}

	@Test
	void searchCompanies_ByIndustryOnly_ReturnsFilteredPage() {
		// Arrange
		String industry = "Tech";

		when(redisService.getListValue(anyString(), any())).thenReturn(null);
		when(redisService.getValue(anyString(), eq(Long.class))).thenReturn(null);

		Page<Company> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);
		when(companyRepository.findByIndustryContaining(industry, pageable)).thenReturn(companyPage);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).setValue(anyString(), any(), anyLong());

		// Act
		Page<CompanyDto> result = companyService.searchCompanies(null, industry, pageable);

		// Assert
		assertEquals(1, result.getTotalElements());
		verify(companyRepository).findByIndustryContaining(industry, pageable);
	}

	@Test
	void findCompanyById_ReturnsFromCacheWhenAvailable() {
		// Arrange
		when(redisService.getValue("company:1", Company.class)).thenReturn(company);

		// Act
		Company result = companyService.findCompanyById(1L);

		// Assert
		assertEquals(company, result);
		verifyNoInteractions(companyRepository);
	}

	@Test
	void findCompanyById_FetchesFromRepositoryWhenCacheEmpty() {
		// Arrange
		when(redisService.getValue("company:1", Company.class)).thenReturn(null);
		when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
		doNothing().when(redisService).setValue(eq("company:1"), eq(company), eq(3600L));

		// Act
		Company result = companyService.findCompanyById(1L);

		// Assert
		assertEquals(company, result);
		verify(companyRepository).findById(1L);
		verify(redisService).setValue("company:1", company, 3600L);
	}

	@Test
	void createCompany_ClearsPaginationCache() {
		// Arrange
		when(companyMapper.convertToEntity(companyDto)).thenReturn(company);
		when(companyRepository.save(company)).thenReturn(company);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).deleteKeysByPattern("*company:page:*");

		// Act
		companyService.createCompany(companyDto);

		// Assert
		verify(redisService).deleteKeysByPattern("*company:page:*");
	}

	@Test
	void updateCompany_ClearsPaginationCache() {
		// Arrange
		when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
		when(companyRepository.save(any())).thenReturn(company);
		when(companyMapper.convertToDto(company)).thenReturn(companyDto);
		doNothing().when(redisService).deleteKeysByPattern("*company:page:*");

		// Act
		companyService.updateCompany(1L, companyDto);

		// Assert
		verify(redisService).deleteKeysByPattern("*company:page:*");
	}

}