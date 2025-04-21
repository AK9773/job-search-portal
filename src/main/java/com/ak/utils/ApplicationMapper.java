package com.ak.utils;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.ak.dto.ApplicationDto;
import com.ak.entity.Application;

@Component
public class ApplicationMapper {

	private final ModelMapper modelMapper;

	public ApplicationMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
		configureMappings();
	}

	private void configureMappings() {
		// Configure custom mappings if needed
		modelMapper.typeMap(Application.class, ApplicationDto.class).addMapping(src -> src.getUser().getId(),
				ApplicationDto::setSeekerId);
		modelMapper.typeMap(Application.class, ApplicationDto.class).addMapping(src -> src.getJob().getId(),
				ApplicationDto::setJobId);

	}

	public ApplicationDto convertToDto(Application application) {
		return modelMapper.map(application, ApplicationDto.class);
	}

	public Application convertToEntity(ApplicationDto applicationDto) {
		return modelMapper.map(applicationDto, Application.class);
	}
}