package org.example.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.entity.Log;

import java.util.List;

public interface LogMapper extends BaseMapper<Log> {

    List<Log> selectByDates(@Param("userId") Long userId, @Param("dateList") List<String> dateList);
}
