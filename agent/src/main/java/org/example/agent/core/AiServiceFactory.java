package org.example.agent.core;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.example.agent.core.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;


@Configuration
public class AiServiceFactory {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Bean
    public TeacherService teacherService() {
        return AiServices.create(TeacherService.class, chatModel);
    }

    @Bean
    public TeacherService teacherServiceStream() {
        return AiServices.create(TeacherService.class, streamingChatModel);
    }
}
