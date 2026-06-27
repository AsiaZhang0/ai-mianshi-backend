package org.example.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.model.entity.Log;

import java.util.List;

public interface LogService extends IService<Log> {

    List<Log> selectByDates(Long userId, List<String> dateList);
}
