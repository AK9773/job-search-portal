package com.ak.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ak.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
	Page<Company> findByNameContaining(String name, Pageable pageable);

	Page<Company> findByIndustryContaining(String industry, Pageable pageable);

	Page<Company> findByNameContainingAndIndustryContaining(String name, String industry, Pageable pageable);
}