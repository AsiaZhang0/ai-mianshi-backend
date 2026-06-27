package org.example.questions.kafka;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.model.dto.question.QuestionQueryRequest;
import org.example.model.entity.Question;
import org.example.questions.service.QuestionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务启动时全量同步题目数据到 agent 模块
 * <p>
 * 逐条发送到 Kafka，由 producer 的 batch.size 自动攒批后一次网络请求发出，
 * 消费者端以 batch 模式拉取并批量生成嵌入向量存入 pgvector。
 * 发送前先发 FULL_SYNC_START 消息通知 agent。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSyncRunner implements ApplicationRunner {

    private final QuestionService questionService;
    private final QuestionEventProducer questionEventProducer;

    private static final int PAGE_SIZE = 200;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 开始全量同步题目数据到 Kafka ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 先发送清空指令
            questionEventProducer.sendFullSyncStart();
            Thread.sleep(2000);

            // 2. 分页拉取所有题目，逐条发送（producer 自动攒批）
            int totalCount = 0;
            int current = 1;
            while (true) {
                QuestionQueryRequest request = new QuestionQueryRequest();
                request.setCurrent(current);
                request.setPageSize(PAGE_SIZE);

                Page<Question> page = questionService.listQuestionByPage(request);
                List<Question> records = page.getRecords();

                if (records == null || records.isEmpty()) {
                    break;
                }

                for (Question question : records) {
                    questionEventProducer.sendQuestionCreated(question);
                }

                totalCount += records.size();
                log.info("全量同步: 发送第 {} 页，本页 {} 条，累计 {} 条", current, records.size(), totalCount);

                if (records.size() < PAGE_SIZE) {
                    break;
                }
                current++;

                Thread.sleep(500);
            }

            long cost = System.currentTimeMillis() - startTime;
            log.info("========== 全量同步完成，共发送 {} 道题目，耗时 {}ms ==========", totalCount, cost);
        } catch (Exception e) {
            log.error("全量同步题目数据失败", e);
        }
    }
}
