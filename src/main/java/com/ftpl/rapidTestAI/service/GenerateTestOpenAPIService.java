package com.ftpl.rapidTestAI.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.File;
import java.io.FileWriter;
import java.util.List;

@Slf4j
@Component
public class GenerateTestOpenAPIService {

    @Autowired
    private OpenAiService openAiService;

    private final double threshold = 80.0;

    public void generateTests(final String localRepositoryPath, final String remoteRepoUrl) {

        // clone the repository with inputs as localRepositoryPath, remoteRepoUrl

        // convert the source to codeIndexer

        // feed the codeIndexer to LLM

        //

    }

    public void generateTestsFromOpenAPISpec(final File specFile,
                                             final String featureOutputFilePath,
                                             final String stepDefOutputFilePath) {
        // 1. Parse OpenAPI spec
        final OpenAPI openAPI = new OpenAPIV3Parser().read(specFile.getAbsolutePath());

        // 2. For each path in the spec, generate tests
        openAPI.getPaths().forEach((path, item) -> {
            try {
                generateFeatureFromOpenAPIPathAndMethod(
                        path,
                        item.readOperationsMap().keySet().iterator().next().toString(),
                        featureOutputFilePath,
                        stepDefOutputFilePath
                );
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        log.info("Test generation complete from OpenAPI Spec File.");
    }

    private void generateFeatureFromOpenAPIPathAndMethod(final String path,
                                                         final String method,
                                                         final String featureOutputFilePath,
                                                         final String stepDefOutputFilePath) throws Exception {
        // Build prompt for LLM
        final String prompt = String.format(
                """
                        Generate a Cucumber feature scenario for %s %s, including Given, When, Then steps, with placeholder parameters.
                        Only return feature file without any meta data details containing only gherkin syntax""",
                method, path
        );

        final ChatMessage systemMessageForFeature = new ChatMessage("system", "You are a test-generator.");
        final ChatMessage userMessageForFeature = new ChatMessage("user", prompt);
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(systemMessageForFeature, userMessageForFeature))
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(request);
        final String featureText = result.getChoices().get(0).getMessage().getContent();

        // 4. Write feature file
        final String fileName = featureOutputFilePath + sanitize(path) + ".feature";
        try (final FileWriter writer = new FileWriter(fileName)) {
            writer.write(featureText);
        }

        // 5. Similarly, request and write step definition
        final String stepPrompt = String.format(
                "Based on this feature scenario, generate Java step definitions using RestAssured and JUnit for %s %s.",
                method, path
        );
        final ChatMessage userMessageForStep = new ChatMessage("user", stepPrompt);
        request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(userMessageForStep))
                .build();
        result = openAiService.createChatCompletion(request);
        final String stepDefs = result.getChoices().get(0).getMessage().getContent();

        final String stepFile = stepDefOutputFilePath + sanitize(path) + "Steps.java";
        try (final FileWriter writer = new FileWriter(stepFile)) {
            writer.write(stepDefs);
        }
    }

    private String sanitize(final String path) {
        return path.replaceAll("[^a-zA-Z0-9]", "_");
    }

}
