package org.example.agent.core.service;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface TeacherService {

    /**
     * 解析简历，提炼用户画像（无需 RAG）
     */
    @SystemMessage("这是一名求职者的简历，请提炼出简历的关键信息。字数严格控制在500字以内。")
    String getPersonalInfo(String message);

    @SystemMessage("这是一名求职者的简历，请提炼出简历的关键信息。字数严格控制在500字以内。")
    Flux<String> getPersonalInfoStream(String message);


}
