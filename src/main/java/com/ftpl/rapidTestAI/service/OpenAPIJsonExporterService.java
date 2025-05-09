package com.ftpl.rapidTestAI.service;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.parser.OpenAPIV3Parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

@Slf4j
@Component
public class OpenAPIJsonExporterService {

    @Autowired
    private OpenAPIChunkExporter chunkExporter;


    public void parseOpenAPIJsonToSummary(final String specFilePath,
                                          final String outputFilePath) {
        final File specFile = new File(specFilePath);
        final OpenAPI openAPI = new OpenAPIV3Parser().read(specFile.getAbsolutePath());

        if (openAPI == null) {
            log.info("Failed to parse OpenAPI spec.");
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode endpointsArray = mapper.createArrayNode();

        for (final Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            final String path = pathEntry.getKey();
            final PathItem item = pathEntry.getValue();

            addOperation(openAPI, endpointsArray, mapper, "GET", path, item.getGet());
            addOperation(openAPI, endpointsArray, mapper, "POST", path, item.getPost());
            addOperation(openAPI, endpointsArray, mapper, "PUT", path, item.getPut());
            addOperation(openAPI, endpointsArray, mapper, "DELETE", path, item.getDelete());
            addOperation(openAPI, endpointsArray, mapper, "PATCH", path, item.getPatch());
        }

        // Output as formatted JSON string
        final String outputFileName = outputFilePath + "openapi_summary.json";
        try {
            final String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(endpointsArray);
            final FileWriter writer = new FileWriter(outputFileName);
            writer.write(jsonOutput);
            writer.close();
        } catch (final Exception e) {
            log.error("error while writing the output file in parseOpenAPIJsonToSummary function", e);
            e.printStackTrace();
        }

        log.info("✅ JSON summary written to openapi_summary.json");

        chunkExporter.OpenAPISummarizedFileToChunks(outputFileName);
    }

    private static void addOperation(final OpenAPI openAPI, final ArrayNode array, final ObjectMapper mapper, final String method, final String path, final Operation op) {
        if (op == null) return;

        final ObjectNode operationNode = mapper.createObjectNode();
        operationNode.put("method", method);
        operationNode.put("path", path);
        operationNode.put("summary", safe(op.getSummary()));
        operationNode.put("description", safe(op.getDescription()));
        operationNode.putPOJO("tags", op.getTags());

        // Request Body
        if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
            final ObjectNode requestBody = mapper.createObjectNode();
            for (final Map.Entry<String, MediaType> entry : op.getRequestBody().getContent().entrySet()) {
                final ObjectNode mediaTypeNode = mapper.createObjectNode();
                mediaTypeNode.set("schema", schemaToJson(mapper, entry.getValue().getSchema(), openAPI));
                mediaTypeNode.set("examples", exampleToJson(mapper, entry.getValue()));
                requestBody.set(entry.getKey(), mediaTypeNode);
            }
            operationNode.set("requestBody", requestBody);
        }

        // Responses
        if (op.getResponses() != null) {
            final ObjectNode responsesNode = mapper.createObjectNode();
            for (final Map.Entry<String, ApiResponse> respEntry : op.getResponses().entrySet()) {
                final ObjectNode responseNode = mapper.createObjectNode();
                responseNode.put("description", safe(respEntry.getValue().getDescription()));

                if (respEntry.getValue().getContent() != null) {
                    final ObjectNode contentNode = mapper.createObjectNode();
                    for (final Map.Entry<String, MediaType> media : respEntry.getValue().getContent().entrySet()) {
                        final ObjectNode mediaNode = mapper.createObjectNode();
                        mediaNode.set("schema", schemaToJson(mapper, media.getValue().getSchema(), openAPI));
                        mediaNode.set("examples", exampleToJson(mapper, media.getValue()));
                        contentNode.set(media.getKey(), mediaNode);
                    }
                    responseNode.set("content", contentNode);
                }

                responsesNode.set(respEntry.getKey(), responseNode);
            }
            operationNode.set("responses", responsesNode);
        }

        // Security
        if (op.getSecurity() != null && !op.getSecurity().isEmpty()) {
            final ArrayNode securityArray = mapper.createArrayNode();
            for (final SecurityRequirement sec : op.getSecurity()) {
                final ObjectNode secNode = mapper.createObjectNode();
                for (final Map.Entry<String, java.util.List<String>> entry : sec.entrySet()) {
                    secNode.putPOJO(entry.getKey(), entry.getValue());
                }
                securityArray.add(secNode);
            }
            operationNode.set("security", securityArray);
        }

        array.add(operationNode);
    }

    private static ObjectNode schemaToJson(final ObjectMapper mapper, final Schema<?> schema) {
        final ObjectNode schemaNode = mapper.createObjectNode();
        if (schema == null) return schemaNode;

        schemaNode.put("type", schema.getType());
        if (schema.getProperties() != null) {
            final ObjectNode props = mapper.createObjectNode();
            for (final Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                props.put(entry.getKey(), entry.getValue().getType());
            }
            schemaNode.set("properties", props);
        }
        return schemaNode;
    }

    private static ObjectNode schemaToJson(final ObjectMapper mapper, final Schema<?> schema, final OpenAPI openAPI) {
        final ObjectNode schemaNode = mapper.createObjectNode();
        if (schema == null) return schemaNode;

        // Handle $ref
        if (schema.get$ref() != null) {
            final String refName = schema.get$ref().replace("#/components/schemas/", "");
            final Schema<?> refSchema = openAPI.getComponents().getSchemas().get(refName);
            if (refSchema != null) {
                schemaNode.put("ref", refName);
                final ObjectNode resolved = schemaToJson(mapper, refSchema, openAPI);
                schemaNode.set("resolved", resolved);
            }
            return schemaNode;
        }

        // Type
        if (schema.getType() != null) {
            schemaNode.put("type", schema.getType());
        }

        // Description
        if (schema.getDescription() != null) {
            schemaNode.put("description", schema.getDescription());
        }

        // Required fields
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            final ArrayNode requiredArray = mapper.createArrayNode();
            for (final String req : schema.getRequired()) {
                requiredArray.add(req);
            }
            schemaNode.set("required", requiredArray);
        }

        // Properties
        if (schema.getProperties() != null) {
            final ObjectNode props = mapper.createObjectNode();
            for (final Map.Entry<String, Schema> prop : schema.getProperties().entrySet()) {
                final ObjectNode propNode = schemaToJson(mapper, prop.getValue(), openAPI);
                // Add property description (if available)
                if (prop.getValue().getDescription() != null) {
                    propNode.put("description", prop.getValue().getDescription());
                }
                props.set(prop.getKey(), propNode);
            }
            schemaNode.set("properties", props);
        }

        // Items (for arrays)
        if (schema.getItems() != null) {
            schemaNode.set("items", schemaToJson(mapper, schema.getItems(), openAPI));
        }

        // allOf
        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            final ArrayNode allOfArray = mapper.createArrayNode();
            for (final Schema<?> s : schema.getAllOf()) {
                allOfArray.add(schemaToJson(mapper, s, openAPI));
            }
            schemaNode.set("allOf", allOfArray);
        }

        // anyOf
        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            final ArrayNode anyOfArray = mapper.createArrayNode();
            for (final Schema<?> s : schema.getAnyOf()) {
                anyOfArray.add(schemaToJson(mapper, s, openAPI));
            }
            schemaNode.set("anyOf", anyOfArray);
        }

        // oneOf
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            final ArrayNode oneOfArray = mapper.createArrayNode();
            for (final Schema<?> s : schema.getOneOf()) {
                oneOfArray.add(schemaToJson(mapper, s, openAPI));
            }
            schemaNode.set("oneOf", oneOfArray);
        }

        return schemaNode;
    }


    private static ObjectNode exampleToJson(final ObjectMapper mapper, final MediaType media) {
        final ObjectNode exampleNode = mapper.createObjectNode();
        if (media.getExample() != null) {
            exampleNode.put("default", media.getExample().toString());
        } else if (media.getExamples() != null) {
            for (final Map.Entry<String, Example> ex : media.getExamples().entrySet()) {
                if (ex.getValue().getValue() != null) {
                    exampleNode.put(ex.getKey(), ex.getValue().getValue().toString());
                }
            }
        }
        return exampleNode;
    }

    private static String safe(final String s) {
        return s == null ? "" : s;
    }
}
