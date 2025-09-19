package com.Human.Ai.D.makit.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class BedrockService {
    
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    public BedrockService() {
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateTextWithClaude(String prompt, int maxTokens) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
            requestBody.put("max_tokens_to_sample", maxTokens);
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.9);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("anthropic.claude-v2")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.get("completion").asText().trim();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate text with Claude: " + e.getMessage(), e);
        }
    }
    
    public String generateTextWithTitan(String prompt, int maxTokens) {
        try {
            Map<String, Object> textGenerationConfig = new HashMap<>();
            textGenerationConfig.put("maxTokenCount", maxTokens);
            textGenerationConfig.put("temperature", 0.7);
            textGenerationConfig.put("topP", 0.9);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputText", prompt);
            requestBody.put("textGenerationConfig", textGenerationConfig);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("amazon.titan-text-express-v1")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.get("results").get(0).get("outputText").asText().trim();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate text with Titan: " + e.getMessage(), e);
        }
    }
    
    public String generateImageWithStableDiffusion(String prompt, int width, int height) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text_prompts", new Object[]{
                Map.of("text", prompt, "weight", 1.0)
            });
            requestBody.put("cfg_scale", 10);
            requestBody.put("seed", 0);
            requestBody.put("steps", 50);
            requestBody.put("width", width);
            requestBody.put("height", height);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("stability.stable-diffusion-xl-v1")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.get("artifacts").get(0).get("base64").asText();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate image with Stable Diffusion: " + e.getMessage(), e);
        }
    }
    
    public String generateEmbedding(String text) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputText", text);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId("amazon.titan-embed-text-v1")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode embedding = jsonResponse.get("embedding");
            
            // Convert embedding array to string representation
            return objectMapper.writeValueAsString(embedding);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Generic text generation method that uses Claude by default
     */
    public String generateText(String prompt, String modelId) {
        if (modelId == null || modelId.contains("claude")) {
            return generateTextWithClaude(prompt, 1000);
        } else if (modelId.contains("titan")) {
            return generateTextWithTitan(prompt, 1000);
        } else {
            // Default to Claude
            return generateTextWithClaude(prompt, 1000);
        }
    }

    /**
     * Simple text generation method with default parameters
     */
    public String generateText(String prompt) {
        return generateTextWithClaude(prompt, 1000);
    }
}