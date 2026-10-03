package com.appointflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiChatRequest {

    private String model;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    private List<Message> messages;

    private List<Tool> tools;

    @JsonProperty("tool_choice")
    private Object toolChoice;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;
        private String content;

        @JsonProperty("tool_call_id")
        private String toolCallId;

        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        @Builder.Default
        private String type = "function";

        private FunctionDef function;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDef {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolCall {
        private String id;

        @Builder.Default
        private String type = "function";

        private FunctionCall function;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionCall {
        private String name;
        private String arguments;
    }
}
