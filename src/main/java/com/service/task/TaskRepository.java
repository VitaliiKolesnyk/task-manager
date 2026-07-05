package com.service.task;

import com.service.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerAndDoneOrderByCreatedAtDesc(User owner, boolean done);

    List<Task> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Task> findByIdAndOwner(Long id, User owner);
}
