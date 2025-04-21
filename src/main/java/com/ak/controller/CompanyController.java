package com.ak.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.CompanyDto;
import com.ak.service.CompanyService;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private CompanyService companyService;

	public CompanyController(CompanyService companyService) {
		super();
		this.companyService = companyService;
	}

	@GetMapping
	public ResponseEntity<Page<CompanyDto>> getAllCompanies(Pageable pageable) {
		return ResponseEntity.ok(companyService.getAllCompanies(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CompanyDto> getCompanyById(@PathVariable Long id) {
		return ResponseEntity.ok(companyService.getCompanyById(id));
	}

	@PostMapping
	public ResponseEntity<CompanyDto> createCompany(@RequestBody CompanyDto companyDto) {
		return ResponseEntity.ok(companyService.createCompany(companyDto));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CompanyDto> updateCompany(@PathVariable Long id, @RequestBody CompanyDto companyDetails) {
		return ResponseEntity.ok(companyService.updateCompany(id, companyDetails));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteCompany(@PathVariable Long id) {
		companyService.deleteCompany(id);
		return ResponseEntity.ok("Company deleted successfully");
	}

	@GetMapping("/search")
	public ResponseEntity<Page<CompanyDto>> searchCompanies(@RequestParam(required = false) String name,
			@RequestParam(required = false) String industry, Pageable pageable) {
		return ResponseEntity.ok(companyService.searchCompanies(name, industry, pageable));
	}
}