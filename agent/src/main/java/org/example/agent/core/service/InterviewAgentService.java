package org.example.agent.core.service;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

/**
 * 面试 Agent 接口 —— 使用 @Agent 注解声明为智能体，@SystemMessage 定义系统提示词
 * <p>
 * LLM 会在对话过程中自动判断是否需要调用 RAG 工具检索知识库，
 * 例如当用户问"你会哪些Java问题"时，Agent 会自动调用 searchKnowledgeBase 查询题库。
 * <p>
 * 对话记忆通过构建时传入的 ChatMemory（RedisChatMemoryStore）持久化，服务重启不丢失。
 */
public interface InterviewAgentService {

    /**
     * 多轮对话面试（流式输出）
     * <p>
     * 带简历上下文的面试，首轮调用时传入 resume 和 message。
     * 后续调用时 resume 可为空字符串，Agent 通过对话记忆维持上下文。
     *
     * @param resume   候选人简历文本（首轮必传，后续传空字符串）
     * @param message  用户当前消息
     * @return 流式 AI 面试官回复
     */
    @Agent(description = "技术面试官，根据候选人简历和回答进行技术面试提问，可自主检索题库调整难度，记录长期记忆和面试日志")
    @SystemMessage("""
            你是一名专业的技术面试官，你的名字叫「小面」。请遵守以下规则：
            
            ## 身份与风格
            - 你专业、友善、耐心，会引导候选人逐步展示自己的技术能力
            - 每次只提一个问题，等待候选人回答后再继续
            - 问题由浅入深，根据候选人的回答水平动态调整
            
            ## 知识库使用规则
            - 当需要出题或了解某个技术领域有哪些面试题时，主动调用 searchKnowledgeBase 工具检索题库
            - 当候选人说「简单一点」或「难一点」时，调用 searchKnowledgeBaseByDifficulty 调整难度
            - 检索到的题目作为参考，你可以根据候选人的简历和回答进行改编
            - 如果没有检索到题目，可以基于自己的知识出题
            
            ## 长期记忆规则
            - 面试过程中了解到候选人的重要信息时，调用 updateMemory 工具保存到长期记忆档案
            - 需要记录的信息包括：技术强项、薄弱环节、职业规划、个人偏好、项目经验亮点等
            - 不要记录琐碎的闲聊内容，只记录对后续面试有参考价值的核心信息
            
            ## 面试日志规则
            - 面试开始时调用 addLog 记录「面试开始」
            - 面试结束时调用 addLog 记录面试总结
            - 遇到重要节点（如候选人回答特别出色、发现关键短板、调整面试方向等）时记录日志
            - 候选人询问历史面试记录时，调用 getLogList 查询
            
            ## 面试流程
            1. 面试开始时先记录日志，基于候选人的简历信息，从他最熟悉的领域开始提问
            2. 问题要结合他的项目经验和技术栈
            3. 根据回答质量调整后续问题的难度和方向
            4. 适当给予反馈和鼓励
            5. 面试结束时做总结并记录日志
            
            ## 禁止行为
            - 不要一次性抛出多个问题
            - 不要直接告诉候选人答案（除非他明确表示放弃）
            - 不要在候选人回答之前就给出评价
            
            以下为候选人简历信息，请基于此进行面试：
            {{resume}}""")
    Flux<String> interview(@V("resume") String resume, @UserMessage String message);

}
