package com.appointflow.ai.client;

import com.appointflow.ai.dto.OpenAiChatRequest;
import com.appointflow.ai.dto.OpenAiChatResponse;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OpenAiApiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final WebClient webClient;
    private final String defaultModel;
    private final int defaultMaxTokens;
    private final int timeoutSeconds;

    public OpenAiApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.model:gpt-4o-mini}") String defaultModel,
            @Value("${app.openai.max-tokens:2048}") int defaultMaxTokens,
            @Value("${app.openai.timeout-seconds:30}") int timeoutSeconds) {

        this.defaultModel = defaultModel;
        this.defaultMaxTokens = defaultMaxTokens;
        this.timeoutSeconds = timeoutSeconds;

        // Reactor Netty'i sistem DNS resolver'ina zorla
        // (varsayilan resolver IPv6/IPv4 paralel sorgularken bazen takiliyor)
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        this.webClient = webClientBuilder
                .baseUrl(OPENAI_API_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();

        log.info("OpenAiApiClient baslatildi — model: {}, maxTokens: {}, timeout: {}s",
                defaultModel, defaultMaxTokens, timeoutSeconds);
    }

    public OpenAiChatResponse sendMessage(String systemPrompt,
                                          List<OpenAiChatRequest.Message> messages) {
        return sendMessage(systemPrompt, messages, defaultModel, defaultMaxTokens, null);
    }

    public OpenAiChatResponse sendMessage(String systemPrompt,
                                          List<OpenAiChatRequest.Message> messages,
                                          String model,
                                          int maxTokens) {
        return sendMessage(systemPrompt, messages, model, maxTokens, null);
    }

    public OpenAiChatResponse sendMessage(String systemPrompt,
                                          List<OpenAiChatRequest.Message> messages,
                                          String model,
                                          int maxTokens,
                                          List<OpenAiChatRequest.Tool> tools) {
        return sendMessage(systemPrompt, messages, model, maxTokens, tools, null);
    }

    public OpenAiChatResponse sendMessage(String systemPrompt,
                                          List<OpenAiChatRequest.Message> messages,
                                          String model,
                                          int maxTokens,
                                          List<OpenAiChatRequest.Tool> tools,
                                          Object toolChoice) {

        // system prompt'u messages basina ekle (OpenAI'de ayri field yok)
        List<OpenAiChatRequest.Message> finalMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            finalMessages.add(OpenAiChatRequest.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build());
        }
        if (messages != null) {
            finalMessages.addAll(messages);
        }

        OpenAiChatRequest request = OpenAiChatRequest.builder()
                .model(model != null ? model : defaultModel)
                .maxTokens(maxTokens > 0 ? maxTokens : defaultMaxTokens)
                .messages(finalMessages)
                .tools(tools)
                .toolChoice(toolChoice)
                .build();

        try {
            log.debug("OpenAI API istegi gonderiliyor — model: {}, mesaj sayisi: {}, tool sayisi: {}",
                    request.getModel(), finalMessages.size(), tools != null ? tools.size() : 0);

            OpenAiChatResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                            .maxBackoff(Duration.ofSeconds(3))
                            .filter(this::isRetryableError))
                    .block();

            if (response != null && response.getUsage() != null) {
                String finishReason = (response.getChoices() != null && !response.getChoices().isEmpty())
                        ? response.getChoices().get(0).getFinishReason()
                        : "unknown";
                log.debug("OpenAI API yanit alindi — input: {} tokens, output: {} tokens, finish: {}",
                        response.getUsage().getInputTokens(),
                        response.getUsage().getOutputTokens(),
                        finishReason);
            }

            return response;

        } catch (WebClientResponseException e) {
            log.error("OpenAI API hatasi — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API hatasi: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("OpenAI API baglanti hatasi: {}", e.getMessage());
            throw new RuntimeException("OpenAI API baglanti hatasi", e);
        }
    }

    private boolean isRetryableError(Throwable t) {
        Throwable current = t;
        for (int i = 0; i < 5 && current != null; i++) {
            if (current instanceof UnknownHostException) return true;
            if (current instanceof java.util.concurrent.TimeoutException) return true;
            if (current instanceof java.net.SocketTimeoutException) return true;
            if (current instanceof java.net.ConnectException) return true;
            if (current instanceof org.springframework.web.reactive.function.client.WebClientRequestException) return true;
            current = current.getCause();
        }
        return false;
    }
}
