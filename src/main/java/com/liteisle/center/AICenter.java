package com.liteisle.center;

import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import com.liteisle.util.AITool;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AICenter {

    // 2. 不再注入具体的 Function，而是注入整个 AITools 服务
    @Resource
    private AITool aiTool;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, JdbcTemplate jdbcTemplate) {

        ChatMemoryRepository chatMemoryRepository = MysqlChatMemoryRepository.mysqlBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(2000)
                .build();

        return builder
                .defaultSystem(
                        """
                        hello
                        """
                )
                // 3. 使用 .defaultTools() 并传入整个服务实例。
                // 框架会自动扫描 aiTools 对象中所有被 @Tool 注解的方法。
//                .defaultTools(aiTool)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}