package org.example.agent;


import jakarta.annotation.Resource;
import org.example.agent.service.impl.QuestionService;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AgentApplicationTest {

    @Resource
    QuestionService questionService;

//    @Test
//    public void testAddWithCustomId() {
//        questionService.addWithCustomId( "什么是Java多态？");
//        System.out.println("插入成功，自定义 ID: question-001");
//    }
}
