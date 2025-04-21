package com.ak.service;

import com.ak.dto.CompanyDto;
import com.ak.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyService {
	Page<CompanyDto> getAllCompanies(Pageable pageable);

	CompanyDto getCompanyById(Long id);

	CompanyDto createCompany(CompanyDto company);

	CompanyDto updateCompany(Long id, CompanyDto companyDetails);

	void deleteCompany(Long id);

	Page<CompanyDto> searchCompanies(String name, String industry, Pageable pageable);

	Company findCompanyById(Long id);
}