package com.ftpl.rapidTestAI.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    public void generateTestsFromOpenAPISpec(final File specFile,
                                             final String featureOutputFilePath,
                                             final String stepDefOutputFilePath) {
        // 1. Parse OpenAPI spec
        final OpenAPI openAPI = new OpenAPIV3Parser().read(specFile.getAbsolutePath());
        String json = "";
        try {
            final ObjectMapper mapper = new ObjectMapper();
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 2. For each path in the spec, generate tests
        final String finalJson = json;
        openAPI.getPaths().forEach((path, item) -> {
            try {
                generateFeatureFromOpenAPIPathAndMethod(
                        finalJson,
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

    private void generateFeatureFromOpenAPIPathAndMethod(final String openAPIJson,
                                                         final String path,
                                                         final String method,
                                                         final String featureOutputFilePath,
                                                         final String stepDefOutputFilePath) throws Exception {
        // Build prompt for LLM
        final String prompt = String.format(
                """
                        Generate a Cucumber feature scenario for %s %s, including Given, When, Then steps, with placeholder parameters. Ensure that you check for all the response params based on the OpenAPI spec.
                        Only return feature file without any meta data details containing only gherkin syntax""",
                method, path
        );
        final String systemPrompt = String.format(
                """
                   You are a test-generator. you understand this openAPI spec file format.
                   %s
                """,
                openAPIJson
        );
        final ChatMessage systemMessageForFeature = new ChatMessage("system", systemPrompt);
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

    public void generateFeaturesFromChunks(final String chunkDirectoryPath,
                                           final String featureOutputFilePath,
                                           final String stepDefOutputFilePath) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final File chunkDir = new File(chunkDirectoryPath);
        final File[] chunkFiles = chunkDir.listFiles((dir, name) -> name.startsWith("openapi_chunk_") && name.endsWith(".json"));

        if (chunkFiles == null || chunkFiles.length == 0) {
            throw new RuntimeException("No chunk files found in " + chunkDirectoryPath);
        }

        for (final File chunkFile : chunkFiles) {
            final ArrayNode endpoints = (ArrayNode) mapper.readTree(chunkFile);
            for (final JsonNode endpoint : endpoints) {
                final String path = endpoint.get("path").asText();
                final String method = endpoint.get("method").asText().toUpperCase(); // e.g., GET, POST
                final String fullContext = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(endpoint);

                generateFeatureFromContext(fullContext, path, method, featureOutputFilePath, stepDefOutputFilePath);
            }
        }
    }

    private void generateFeatureFromContext(final String contextJson,
                                            final String path,
                                            final String method,
                                            final String featureOutputFilePath,
                                            final String stepDefOutputFilePath) throws Exception {
        log.info("Generating feature for path: {} and method: {}", path, method);

        final String prompt = String.format(
                """
                Generate a Cucumber feature scenario for %s %s, including Given, When, Then steps, with placeholder parameters.
                Ensure that you check for all the response params based on the OpenAPI spec.
                Requirements:
                1. Feature file must follow Gherkin syntax precisely
                2. Include scenarios for:
                   - Happy path (200 responses)
                   - Invalid inputs (400 responses)
                   - Not found cases (404 responses)
                   - Edge cases (empty inputs, boundary values)
                3. Organize scenarios logically with descriptive names
                4. Include data tables where appropriate for test cases
                5. Cover all validation rules and business logic
                6. Return ONLY the pure Gherkin syntax content without any markdown, explanations or code blocks
                """,
                method, path
        );

        final String systemPrompt = String.format(
                """
                You are a test-generator. You understand this OpenAPI spec format. Here's the endpoint summary context:
                %s
                """, contextJson
        );

        final ChatMessage systemMessage = new ChatMessage("system", systemPrompt);
        final ChatMessage userMessage = new ChatMessage("user", prompt);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(systemMessage, userMessage))
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(request);
        final String featureText = result.getChoices().get(0).getMessage().getContent();

        final String featureFile = featureOutputFilePath + sanitize(path) + ".feature";
        try (final FileWriter writer = new FileWriter(featureFile)) {
            writer.write(featureText);
        }

        // Generate Step Definitions
        final String stepPrompt = String.format(
                """
                        Generate Java-based Cucumber step definition classes for the following Cucumber `.feature` file as below:
                        %s
                        and path: %s
                        and method: %s
                        Only return the Java code. Do not include any explanations or markdown formatting.
                """, featureText, method, path
        );
        final ChatMessage stepMessage = new ChatMessage("user", stepPrompt);
        request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(stepMessage))
                .build();

        result = openAiService.createChatCompletion(request);
        final String stepText = result.getChoices().get(0).getMessage().getContent();

        final String stepFile = stepDefOutputFilePath + sanitize(path) + "Steps.java";
        try (final FileWriter writer = new FileWriter(stepFile)) {
            writer.write(stepText);
        }
    }


}
