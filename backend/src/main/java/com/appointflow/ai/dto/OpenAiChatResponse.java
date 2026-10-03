package com.appointflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatResponse {

    private static final ObjectMapper ARG_MAPPER = new ObjectMapper();

    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Function {
            private String name;
            private String arguments;
        }

        public String getName() {
            return function != null ? function.getName() : null;
        }

        public Map<String, Object> getInputAsMap() {
            if (function == null || function.getArguments() == null || function.getArguments().isBlank()) {
                return new HashMap<>();
            }
            try {
                return ARG_MAPPER.readValue(function.getArguments(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Tool arguments parse hatasi: {} — args: {}", e.getMessage(), function.getArguments());
                return new HashMap<>();
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int inputTokens;

        @JsonProperty("completion_tokens")
        private int outputTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    public String getTextContent() {
        if (choices == null || choices.isEmpty()) return "";
        Message m = choices.get(0).getMessage();
        if (m == null || m.getContent() == null) return "";
        return m.getContent();
    }

    public List<ToolCall> getToolCalls() {
        if (choices == null || choices.isEmpty()) return List.of();
        Message m = choices.get(0).getMessage();
        if (m == null || m.getToolCalls() == null) return List.of();
        return m.getToolCalls();
    }

    public boolean hasToolCalls() {
        if (choices == null || choices.isEmpty()) return false;
        return "tool_calls".equals(choices.get(0).getFinishReason());
    }
}
