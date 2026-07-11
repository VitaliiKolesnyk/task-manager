package com.service.task;

import com.service.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Selects the owner's tasks filtered by completion status, newest first.
     * Owner-scoped so users only ever see their own tasks. Returns an empty
     * list (never {@code null}) when there are no matches.
     */
    List<Task> findByOwnerAndDoneOrderByCreatedAtDesc(User owner, boolean done);

    /**
     * Selects all of the owner's tasks, newest first. Owner-scoped; returns an
     * empty list (never {@code null}) when the owner has no tasks.
     */
    List<Task> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Fetches a single task by id, scoped to its owner. A foreign or unknown id
     * yields an empty {@link Optional} so callers can return 404 without leaking
     * the existence of other users' tasks.
     */
    Optional<Task> findByIdAndOwner(Long id, User owner);
}
