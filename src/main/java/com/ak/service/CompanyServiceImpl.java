package com.ak.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ak.dto.CompanyDto;
import com.ak.entity.Company;
import com.ak.exception.ResourceNotFoundException;
import com.ak.repository.CompanyRepository;
import com.ak.utils.CompanyMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class CompanyServiceImpl implements CompanyService {

	private CompanyRepository companyRepository;

	private CompanyMapper companyMapper;

	private RedisService redisService;

	public CompanyServiceImpl(CompanyRepository companyRepository, CompanyMapper companyMapper,
			RedisService redisService) {
		this.companyRepository = companyRepository;
		this.companyMapper = companyMapper;
		this.redisService = redisService;
	}

	@Override
	public Page<CompanyDto> getAllCompanies(Pageable pageable) {
		String cacheKey = String.format("company:page:%d:size:%d:sort:%s", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());
		List<CompanyDto> cachedContent = redisService.getListValue(cacheKey + ":content",
				new TypeReference<List<CompanyDto>>() {
				});
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);
		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}
		Page<Company> page = companyRepository.findAll(pageable);
		List<CompanyDto> content = page.map(companyMapper::convertToDto).getContent();
		redisService.setValue(cacheKey + ":content", content, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	@Override
	public CompanyDto getCompanyById(Long id) {
		Company company = this.findCompanyById(id);
		return companyMapper.convertToDto(company);
	}

	@Override
	public CompanyDto createCompany(CompanyDto companyDto) {
		Company company = companyMapper.convertToEntity(companyDto);
		Company savedCompany = companyRepository.save(company);
		CompanyDto dto = companyMapper.convertToDto(savedCompany);
		redisService.deleteKeysByPattern("*company:page:*");
		redisService.setValue("company:" + savedCompany.getId(), dto, 3600L);
		return dto;
	}

	@Override
	public CompanyDto updateCompany(Long id, CompanyDto companyDetails) {
		Company company = this.findCompanyById(id);
		company.setName(companyDetails.getName());
		company.setDescription(companyDetails.getDescription());
		company.setIndustry(companyDetails.getIndustry());
		company.setWebsite(companyDetails.getWebsite());
		Company updatedCompany = companyRepository.save(company);

		CompanyDto dto = companyMapper.convertToDto(updatedCompany);
		redisService.deleteKeysByPattern("*company:page:*");
		redisService.deleteKey("company:" + updatedCompany.getId());
		redisService.setValue("company:" + updatedCompany.getId(), dto, 3600L);
		return dto;
	}

	@Override
	public void deleteCompany(Long id) {
		redisService.deleteKey("company:" + id);
		companyRepository.deleteById(id);
	}

	@Override
	public Page<CompanyDto> searchCompanies(String name, String industry, Pageable pageable) {

		String cacheKey = String.format("company:search:name:%s:industry:%s:page:%d:size:%d:sort:%s",
				name != null ? name : "null", industry != null ? industry : "null", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		List<CompanyDto> cachedContent = redisService.getListValue(cacheKey + ":content",
				new TypeReference<List<CompanyDto>>() {
				});
		Long totalElements = redisService.getValue(cacheKey + ":total", Long.class);

		if (cachedContent != null && totalElements != null) {
			return new PageImpl<>(cachedContent, pageable, totalElements);
		}

		Page<Company> page;

		if (name != null && industry != null) {
			page = companyRepository.findByNameContainingAndIndustryContaining(name, industry, pageable);
		} else if (name != null) {
			page = companyRepository.findByNameContaining(name, pageable);
		} else if (industry != null) {
			page = companyRepository.findByIndustryContaining(industry, pageable);
		} else {
			page = companyRepository.findAll(pageable);
		}
		List<CompanyDto> pageDto = page.map(companyMapper::convertToDto).getContent();
		redisService.setValue(cacheKey + ":content", pageDto, 3600L);
		redisService.setValue(cacheKey + ":total", page.getTotalElements(), 3600L);
		return new PageImpl<>(pageDto, pageable, page.getTotalElements());
	}

	@Override
	public Company findCompanyById(Long id) {
		String cacheKey = "company:" + id;
		Company cachedContent = redisService.getValue(cacheKey, Company.class);
		if (cachedContent != null) {
			return cachedContent;
		}
		Company company = companyRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
		redisService.setValue(cacheKey, company, 3600L);
		return company;
	}
}