package com.ak.utils;

import com.ak.dto.CompanyDto;
import com.ak.entity.Company;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    private final ModelMapper modelMapper;

    public CompanyMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        configureMappings();
    }

    private void configureMappings() {
        

        // Configure mappings from CompanyDto to Company
        modelMapper.typeMap(CompanyDto.class, Company.class)
            .addMappings(mapper -> {
                mapper.skip(Company::setId); // Skip ID to prevent accidental updates
            });
    }

    public CompanyDto convertToDto(Company company) {
        if (company == null) {
            return null;
        }
        return modelMapper.map(company, CompanyDto.class);
    }

    public Company convertToEntity(CompanyDto companyDto) {
        if (companyDto == null) {
            return null;
        }
        return modelMapper.map(companyDto, Company.class);
    }
}
