package org.example.agent.core;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import org.example.agent.core.service.InterviewAgentService;
import org.example.agent.core.tools.LogTool;
import org.example.agent.core.tools.MemoryTool;
import org.example.agent.core.tools.RagTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
public class AgentTest {
    @Resource
    private  StreamingChatModel streamingChatModel;
    @Resource
    private  RagTool ragTool;
    @Resource
    private  LogTool logTool;
    @Resource
    private  MemoryTool memoryTool;
    @Resource
    private  RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private InterviewAgentConfig interviewAgentConfig;
//    @Test
//    public void testAgent() throws InterruptedException {
//        long startTime = System.currentTimeMillis();
//
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .maxMessages(20)
//                .chatMemoryStore(redisChatMemoryStore)
//                .build();
//
//        InterviewAgentService agent = AgenticServices.agentBuilder(InterviewAgentService.class)
//                .streamingChatModel(streamingChatModel)
//                .tools(ragTool, logTool, memoryTool)
//                .chatMemory(chatMemory)
//                .build();
//
//        long buildEndTime = System.currentTimeMillis();
//        System.out.printf("[耗时] Agent 构建: %d ms%n", buildEndTime - startTime);
//
//        CountDownLatch latch = new CountDownLatch(1);
//        long firstCallStart = System.currentTimeMillis();
//
//        new Thread(()->{
//            long t1 = System.currentTimeMillis();
//            agent.interview("1","**基本信息**：张志明，应届硕士，求职意向AI应用开发工程师，意向地武汉/深圳。**教育背景**：三峡大学计算机技术硕士（2023.9-2026.6），计算机科学与技术本科（2018.9-2022.6）。**核心技术**：精通Java后端（JUC/JVM/MySQL/微服务）；熟练掌握Spring AI、LangChain4j等AI框架；熟悉Vue/React前端、Redis/Kafka/ES中间件及Docker部署；掌握PyTorch、机器学习/深度学习算法及Scrapy/Pandas数据处理。**项目科研**：主导4个核心项目。含基于多模型路由与微服务的AI零代码平台（高可用限流+监控）；基于多级缓存与Pulsar的高并发点赞系统（TPS提升至1000+）；基于ES与Redis的面试刷题平台；全栈农田重金属监测APP（ArcGIS可视化+数据加密）。于北京农林科学院联培期间负责系统开发、数据爬取建模，完成硕士论文，获软著与专利各1项。**荣誉优势**：英语六级，获蓝桥杯省三等奖、数学竞赛省二等奖。自学与抗压能力强，深度实践AI辅助开发，擅长技术业务化落地，注重技术钻研与跨团队沟通。"
//            ,"你好");
//            System.out.printf("[耗时] 第1次 interview 调用 (memoryId=1): %d ms%n", System.currentTimeMillis() - t1);
//            latch.countDown();
//        }).start();
//
//        long t2 = System.currentTimeMillis();
//        agent.interview("2","**基本信息**：张志明，应届硕士，求职意向AI应用开发工程师，意向地武汉/深圳。**教育背景**：三峡大学计算机技术硕士（2023.9-2026.6），计算机科学与技术本科（2018.9-2022.6）。**核心技术**：精通Java后端（JUC/JVM/MySQL/微服务）；熟练掌握Spring AI、LangChain4j等AI框架；熟悉Vue/React前端、Redis/Kafka/ES中间件及Docker部署；掌握PyTorch、机器学习/深度学习算法及Scrapy/Pandas数据处理。**项目科研**：主导4个核心项目。含基于多模型路由与微服务的AI零代码平台（高可用限流+监控）；基于多级缓存与Pulsar的高并发点赞系统（TPS提升至1000+）；基于ES与Redis的面试刷题平台；全栈农田重金属监测APP（ArcGIS可视化+数据加密）。于北京农林科学院联培期间负责系统开发、数据爬取建模，完成硕士论文，获软著与专利各1项。**荣誉优势**：英语六级，获蓝桥杯省三等奖、数学竞赛省二等奖。自学与抗压能力强，深度实践AI辅助开发，擅长技术业务化落地，注重技术钻研与跨团队沟通。"
//                ,"你好");
//        System.out.printf("[耗时] 第2次 interview 调用 (memoryId=2): %d ms%n", System.currentTimeMillis() - t2);
//
////        latch.await();
//
//        long totalTime = System.currentTimeMillis() - startTime;
//        System.out.printf("[耗时] 总执行时间: %d ms%n", totalTime);
//    }

    @Test
    public void testAgent2() throws InterruptedException {
        long startTime = System.currentTimeMillis();



        CountDownLatch latch = new CountDownLatch(1);
        long firstCallStart = System.currentTimeMillis();
        InterviewAgentService service1 = interviewAgentConfig.getOrCreateAgent("1");
        InterviewAgentService service2 = interviewAgentConfig.getOrCreateAgent("2");
        new Thread(()->{
            long t1 = System.currentTimeMillis();
            service1.interview("**基本信息**：张志明，应届硕士，求职意向AI应用开发工程师，意向地武汉/深圳。**教育背景**：三峡大学计算机技术硕士（2023.9-2026.6），计算机科学与技术本科（2018.9-2022.6）。**核心技术**：精通Java后端（JUC/JVM/MySQL/微服务）；熟练掌握Spring AI、LangChain4j等AI框架；熟悉Vue/React前端、Redis/Kafka/ES中间件及Docker部署；掌握PyTorch、机器学习/深度学习算法及Scrapy/Pandas数据处理。**项目科研**：主导4个核心项目。含基于多模型路由与微服务的AI零代码平台（高可用限流+监控）；基于多级缓存与Pulsar的高并发点赞系统（TPS提升至1000+）；基于ES与Redis的面试刷题平台；全栈农田重金属监测APP（ArcGIS可视化+数据加密）。于北京农林科学院联培期间负责系统开发、数据爬取建模，完成硕士论文，获软著与专利各1项。**荣誉优势**：英语六级，获蓝桥杯省三等奖、数学竞赛省二等奖。自学与抗压能力强，深度实践AI辅助开发，擅长技术业务化落地，注重技术钻研与跨团队沟通。"
            ,"你好")
            .subscribe(
                token -> System.out.print("[1] " + token),
                err -> System.err.println("[错误-1] " + err.getMessage()),
                () -> System.out.println("\n[完成-1] interview 流式输出结束")
            );
            System.out.printf("[耗时] 第1次 interview 调用 (memoryId=1): %d ms%n", System.currentTimeMillis() - t1);
            latch.countDown();
        }).start();

        long t2 = System.currentTimeMillis();
        Flux<String> interview = service2.interview("**基本信息**：张志明，应届硕士，求职意向AI应用开发工程师，意向地武汉/深圳。**教育背景**：三峡大学计算机技术硕士（2023.9-2026.6），计算机科学与技术本科（2018.9-2022.6）。**核心技术**：精通Java后端（JUC/JVM/MySQL/微服务）；熟练掌握Spring AI、LangChain4j等AI框架；熟悉Vue/React前端、Redis/Kafka/ES中间件及Docker部署；掌握PyTorch、机器学习/深度学习算法及Scrapy/Pandas数据处理。**项目科研**：主导4个核心项目。含基于多模型路由与微服务的AI零代码平台（高可用限流+监控）；基于多级缓存与Pulsar的高并发点赞系统（TPS提升至1000+）；基于ES与Redis的面试刷题平台；全栈农田重金属监测APP（ArcGIS可视化+数据加密）。于北京农林科学院联培期间负责系统开发、数据爬取建模，完成硕士论文，获软著与专利各1项。**荣誉优势**：英语六级，获蓝桥杯省三等奖、数学竞赛省二等奖。自学与抗压能力强，深度实践AI辅助开发，擅长技术业务化落地，注重技术钻研与跨团队沟通。"
                , "你好");
        interview.subscribe(
                token -> System.out.print(token),
                err -> System.err.println("[错误] " + err.getMessage()),
                () -> System.out.println("\n[完成] interview 流式输出结束")
        );
        System.out.printf("[耗时] 第2次 interview 调用 (memoryId=2): %d ms%n", System.currentTimeMillis() - t2);

        latch.await();

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("[耗时] 总执行时间: %d ms%n", totalTime);
    }
}
