package org.example.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.example.agent.mapper.LogMapper;
import org.example.agent.service.LogService;
import org.example.model.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
    private LogMapper logMapper;

    @Override
    public List<Log> selectByDates(Long userId, List<String> dateList) {
        return logMapper.selectByDates(userId, dateList);
    }
}
