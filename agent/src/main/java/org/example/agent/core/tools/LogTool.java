package org.example.agent.core.tools;



import cn.dev33.satoken.stp.StpUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.agent.service.LogService;
import org.example.model.entity.Log;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class LogTool {

    private final LogService logService;

    @Tool("记录面试过程中的关键事件日志。当发生以下情况时调用：1) 面试开始或结束 2) 候选人回答质量特别好或特别差 3) 候选人表现出某个技术领域的强项或短板 4) 面试中有重要转折或决策。日志内容应简洁扼要，一句话说清楚事件即可")
    public boolean addLog(@P("日志内容") String content){
        Log log = new Log();
        log.setUserId((Long) StpUtil.getLoginId());
        log.setContent(content);
        log.setCreateTime(new Date());

        return logService.save(log);
    }

    @Tool("查询指定日期的面试日志列表。当候选人询问「之前的面试记录」或需要回顾历史面试情况时调用。dateList 为日期列表，格式如 2025-06-01")
    public List<Log> getLogList(@P("日期列表,日期格式:2025-06-01") List<String> dateList){
        return logService.selectByDates((Long) StpUtil.getLoginId(), dateList);
    }
}
