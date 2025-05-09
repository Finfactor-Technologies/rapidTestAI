package com.ftpl.rapidTestAI.config;

import com.theokanning.openai.service.OpenAiService;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OpenAIServiceClient {

    @Bean
    public OpenAiService OpenAIService() {
        final String openAiKey = "";
        return new OpenAiService(openAiKey, Duration.ofMinutes(5));
    }

}
