package com.ssergionp.taskmanagerapi.repository;

import com.ssergionp.taskmanagerapi.model.Task;
import com.ssergionp.taskmanagerapi.model.TaskStatus;
import com.ssergionp.taskmanagerapi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByOwner(User owner, Pageable pageable);

    Page<Task> findByOwnerAndStatus(User owner, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndOwner(Long id, User owner);
}
