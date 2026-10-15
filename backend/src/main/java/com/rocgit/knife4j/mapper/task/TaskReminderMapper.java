package com.rocgit.knife4j.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.task.TaskReminder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskReminderMapper extends BaseMapper<TaskReminder> {
}
