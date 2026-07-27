package com.ssergionp.taskmanagerapi.repository;

import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}
