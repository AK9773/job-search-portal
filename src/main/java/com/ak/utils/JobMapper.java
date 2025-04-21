package com.ak.utils;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.ak.dto.JobDto;
import com.ak.entity.Job;

@Component
public class JobMapper {

	private ModelMapper modelMapper;

	public JobMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
		configureMappings();
	}

	private void configureMappings() {

		modelMapper.typeMap(Job.class, JobDto.class).addMapping(src -> src.getCompany().getId(), JobDto::setCompanyId);
		modelMapper.typeMap(Job.class, JobDto.class).addMapping(src -> src.getPostedBy().getId(),
				JobDto::setEmployerId);

		// Configure mappings from CompanyDto to Company
		modelMapper.typeMap(JobDto.class, Job.class).addMappings(mapper -> {
			mapper.skip(Job::setId); // Skip ID to prevent accidental updates
		});
	}

	public JobDto convertToDto(Job job) {
		return modelMapper.map(job, JobDto.class);
	}

	public Job convertToEntity(JobDto jobDto) {
		return modelMapper.map(jobDto, Job.class);
	}

}
