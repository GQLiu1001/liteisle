package com.liteisle.controller;

import com.liteisle.common.Result;
import com.liteisle.common.domain.request.TranslateReq;
import com.liteisle.common.domain.response.TranslateResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/translate")
@Tag(name = "划词翻译接口")
public class TranslateController {

    @Resource
    private ChatClient chatClient;

    /**
     * 划词翻译
     * @param req 包含待翻译文本和源文件名的请求体
     * @return 包含翻译结果的响应
     */
    @Operation(summary = "划词翻译", description = "接收文本和可选的文件名上下文，返回翻译结果")
    @PostMapping
    public Result<TranslateResp> translate(@RequestBody TranslateReq req) {
        String textToTranslate = req.getText();
        String fileName = req.getFileName();
        String targetLang = "中文";

        // --- 输入校验 ---
        if (!StringUtils.hasText(textToTranslate)) {
            return Result.fail("输入文本为空");
        }
        if (!StringUtils.hasText(fileName)) {
            return Result.fail("输入标题为空");
        }

        // --- 构建用户提示词 ---
        // 将待翻译文本和文件名上下文清晰地组合成一个 prompt
        String userMessage = String.format(
                "Please translate the following text into %s. The text is from the file named '%s'.\n\nText: \"%s\"",
                targetLang,
                fileName,
                textToTranslate
        );

        // --- 调用 AI 模型 ---
        try {
            String translatedText = chatClient.prompt()
                    .user(userMessage)
                    // 来自同一个文件的翻译请求都会共享相同的聊天记忆，有助于保持上下文一致性
                    .advisors(a -> a.param("chat_memory_conversation_id", fileName))
                    .call()
                    .content();

            // --- 封装并返回结果 ---
            TranslateResp response = new TranslateResp();
            response.setTranslatedText(translatedText);
            response.setOriginalText(textToTranslate);
            return Result.success(response);

        } catch (Exception e) {
            // 异常处理，可以记录日志
             log.error("翻译文件{}出错", fileName, e);
            return Result.fail("翻译失败");
        }
    }
}
