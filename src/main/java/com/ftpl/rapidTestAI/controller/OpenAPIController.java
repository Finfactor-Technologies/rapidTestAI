package com.ftpl.rapidTestAI.controller;

import com.ftpl.rapidTestAI.service.OpenAPIJsonExporterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for OpenAPI specification related operations
 */
@RestController
@RequestMapping("/rapid-test-ai/api/openapi")
public class OpenAPIController {

    @Autowired
    private OpenAPIJsonExporterService openAPIJsonExporter;


    /**
     * Export OpenAPI specification to a JSON file
     * 
     * @param specFilePath Path to the OpenAPI specification file
     * @param outputFilePath Path where the output JSON summary should be written
     * @return ResponseEntity with a success message or error details
     */
    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> exportOpenAPIToJson(
            @RequestParam final String specFilePath,
            @RequestParam final String outputFilePath) {
        
        final Map<String, String> response = new HashMap<>();

        openAPIJsonExporter.parseOpenAPIJsonToSummary(specFilePath, outputFilePath);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }


}
