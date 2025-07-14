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

    // 如果未来需要使用 Function Calling，可以保留 AITool 的注入
    @Resource
    private AITool aiTool;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, JdbcTemplate jdbcTemplate) {

        // --- 聊天记忆配置 ---
        // 使用 MySQL 作为聊天记录的持久化存储
        ChatMemoryRepository chatMemoryRepository = MysqlChatMemoryRepository.mysqlBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();

        // 配置一个消息窗口聊天记忆，它使用上面的仓库
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(2000) // 设置每个会话保留的最大消息数
                .build();

        // --- 构建 ChatClient ---
        return builder
                // 关键改动：设置一个更智能的系统提示词 (System Prompt)
                .defaultSystem(
                        """
                        You are a professional translator. Your task is to accurately translate the user-provided text into the specified target language.
                        To enhance translation quality, you will receive the source file name ('fileName') along with the text.
                        Please use this file name to infer the document's context (e.g., technical manual, legal contract, academic paper, or casual conversation).
                        This contextual understanding should help you choose the most appropriate terminology and tone.
                        Your output should ONLY be the translated text, without any additional explanations, comments, or introductory phrases.
                        """
                )
                // 如果需要启用 Function Calling，可以取消这行注释
                // .defaultTools(aiTool)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), // 添加日志顾问，方便调试
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // 添加聊天记忆顾问
                )
                .build();
    }
}
