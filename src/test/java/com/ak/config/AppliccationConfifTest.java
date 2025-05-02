package com.ak.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = ApplicationConfig.class)
class ApplicationConfigSpringTest {

	private ApplicationContext context;

	public ApplicationConfigSpringTest(ApplicationContext context) {
		this.context = context;
	}

	@Test
	void testModelMapperBean() {
		ModelMapper modelMapper = context.getBean(ModelMapper.class);
		assertNotNull(modelMapper, "ModelMapper bean should not be null");

		assertTrue(context.isTypeMatch("modelMapper", ModelMapper.class), "Bean should be of type ModelMapper");
	}
}