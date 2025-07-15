package com.liteisle.service.business;

import com.liteisle.common.domain.request.TranslateReq;
import com.liteisle.common.domain.response.TranslateResp;
import com.liteisle.common.exception.LiteisleException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
@Slf4j
@Service
public class TranslateServiceImpl implements  TranslateService{
    @Resource
    private ChatClient chatClient;

    @Override
    public TranslateResp translate(TranslateReq req) {
        String textToTranslate = req.getText();
        String fileName = req.getFileName();
        String targetLang = "中文"; // 未来可改为从配置或req中获取

        // 输入校验（也可移至Controller通过注解校验，如@Valid）
        if (!StringUtils.hasText(textToTranslate)) {
            throw new LiteisleException("输入文本为空");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new LiteisleException("输入标题为空");
        }

        // 构建提示词
        String userMessage = String.format(
                "Please translate the following text into %s. The text is from the file named '%s'.\n\nText: \"%s\"",
                targetLang, fileName, textToTranslate
        );

        // 调用AI模型
        String translatedText;
        try {
            translatedText = chatClient.prompt()
                    .user(userMessage)
                    .advisors(a -> a.param("chat_memory_conversation_id", fileName))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("翻译文件{}出错", fileName, e);
            throw new LiteisleException("翻译失败");
        }

        // 封装结果
        TranslateResp response = new TranslateResp();
        response.setTranslatedText(translatedText);
        response.setOriginalText(textToTranslate);
        return response;
    }
}
