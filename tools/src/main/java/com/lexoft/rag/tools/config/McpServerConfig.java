package com.lexoft.rag.tools.config;

import com.lexoft.rag.tools.tool.DocumentAccessTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class McpServerConfig {

    // ToolCallbackConverterAutoConfiguration.syncTools() injects List<ToolCallback> directly;
    // a ToolCallback[] bean is never picked up — must be a List.
    @Bean
    public List<ToolCallback> documentToolCallbacks(DocumentAccessTool documentAccessTool) {
        return Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(documentAccessTool)
                .build()
                .getToolCallbacks());
    }
}
