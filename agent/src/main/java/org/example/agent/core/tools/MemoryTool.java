package org.example.agent.core.tools;


import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.example.client.service.InnerUserService;
import org.example.model.entity.User;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MemoryTool {

    @DubboReference(check = false, retries = 0, timeout = 5000)
    private InnerUserService innerUserService;

    @Tool("更新候选人的长期记忆档案。当你在面试过程中了解到候选人的重要信息时（如技术偏好、职业规划、个人特点、项目经验总结等），调用此工具将其持久化保存，以便后续面试中参考。注意：仅记录与面试相关的、有价值的信息，不要记录琐碎闲聊")
    public String updateMemory(Long userId, String content){
        User user = new User();
        user.setId(userId);
        user.setUserProfile(content);
        try {
            innerUserService.updateById(user);
            log.info("记忆更新成功: userId={}", userId);
        } catch (Exception e) {
            // user 服务不可用时，记录日志但不阻断面试流程
            log.warn("记忆更新失败（user 服务可能未启动），userId={}, content={}, error={}",
                    userId, content, e.getMessage());
        }
        return content;
    }

}
