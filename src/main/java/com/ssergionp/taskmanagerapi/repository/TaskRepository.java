package com.ssergionp.taskmanagerapi.repository;

import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwner(User owner);

    List<Task> findByOwnerAndStatus(User owner, TaskStatus status);

    Optional<Task> findByIdAndOwner(Long id, User owner);
}
