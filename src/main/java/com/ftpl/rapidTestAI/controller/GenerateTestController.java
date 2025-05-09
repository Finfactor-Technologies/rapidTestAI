package com.ftpl.rapidTestAI.controller;


import com.ftpl.rapidTestAI.service.GenerateTestOpenAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rapid-test-ai/api/generate-test")
public class GenerateTestController {

    @Autowired
    private GenerateTestOpenAPIService generateTestOpenAPIService;

    @PostMapping("/openapi-chunk-files")
    public ResponseEntity<Object> generateTestsFromOpenAPI(@RequestParam final String chunkDirectoryPath,
                                                   @RequestParam final String featureOutputFilePath,
                                                   @RequestParam final String stepDefOutputFilePath) throws Exception {
        generateTestOpenAPIService.generateFeaturesFromChunks(chunkDirectoryPath, featureOutputFilePath, stepDefOutputFilePath);
        return ResponseEntity.ok().build();
    }
}
