package com.rocgit.knife4j.service.task;

import com.rocgit.knife4j.dto.task.*;
import com.rocgit.knife4j.entity.task.TaskVO;

import java.util.List;
import java.util.Map;

public interface TaskService {

    TaskVO create(Long userId, CreateTaskRequest request);

    Map<String, Object> list(Long projectId, Long assigneeId, String status);

    Map<String, Object> myTasks(Long userId, String status, Boolean overdue, int pageNum, int pageSize);

    List<TaskVO> dueSoon(int days);

    TaskVO detail(Long taskId);

    void update(Long taskId, UpdateTaskRequest request);

    void delete(Long taskId);

    void complete(Long taskId, CompleteTaskRequest request);

    void abandon(Long taskId, AbandonTaskRequest request);

    void transfer(Long taskId, TransferTaskRequest request);

    void updateProgress(Long taskId, TaskProgressRequest request);
}