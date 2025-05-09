package com.ftpl.rapidTestAI.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Slf4j
@Component
public class OpenAPIChunkExporter {

    private static final int CHUNK_SIZE = 5; // You can adjust based on your token budget
    private static final String CHUNK_DIR = "chunks";

    public void OpenAPISummarizedFileToChunks(final String summarizedFilePath) {
        try {
            final File inputFile = new File(summarizedFilePath);
            final File chunkDir = new File(CHUNK_DIR);
            if (!chunkDir.exists()) {
                chunkDir.mkdirs();
            }


            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode rootNode = mapper.readTree(inputFile);

            if (!rootNode.isArray()) {
                log.error("Invalid JSON format: expected an array");
                return;
            }

            final ArrayNode array = (ArrayNode) rootNode;
            final int total = array.size();
            final int chunkCount = (int) Math.ceil(total / (double) CHUNK_SIZE);

            for (int i = 0; i < chunkCount; i++) {
                final int start = i * CHUNK_SIZE;
                final int end = Math.min(start + CHUNK_SIZE, total);

                final ArrayNode chunk = mapper.createArrayNode();
                for (int j = start; j < end; j++) {
                    chunk.add(array.get(j));
                }

                final String chunkFilename = "chunks/openapi_chunk_" + (i + 1) + ".json";
                try (final BufferedWriter writer = new BufferedWriter(new FileWriter(chunkFilename))) {
                    writer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chunk));
                }

                log.info("✅ Wrote chunk {} to {}", i + 1, chunkFilename);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
