package com.ftpl.rapidTestAI;

import com.ftpl.rapidTestAI.service.GenerateTestOpenAPIService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;

@SpringBootApplication
public class RapidTestAiApplication {

	public static void main(final String[] args) {
		final ApplicationContext context = SpringApplication.run(RapidTestAiApplication.class, args);

		final String specFilePath = "/Users/darshbadodariya/Documents/finvu/morningstar-data-provider/swagger.json";
		final String featureOutputFilePath = "/Users/darshbadodariya/Documents/finvu/rapidTestAI/";
		final String stepDefOutputFilePath = "/Users/darshbadodariya/Documents/finvu/rapidTestAI/";

		final File specFile = new File(specFilePath);
		final GenerateTestOpenAPIService generateTestOpenAPIService = context.getBean(GenerateTestOpenAPIService.class);
		generateTestOpenAPIService.generateTestsFromOpenAPISpec(specFile, featureOutputFilePath, stepDefOutputFilePath);
	}

}
