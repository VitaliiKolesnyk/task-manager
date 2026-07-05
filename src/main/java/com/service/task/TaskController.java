package com.service.task;

import com.service.user.User;
import com.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository repository;
    private final UserRepository userRepository;

    @GetMapping
    public List<Task> list(@RequestParam(name = "includeDone", defaultValue = "false") boolean includeDone,
                           Principal principal) {
        User owner = currentUser(principal);
        if (includeDone) {
            return repository.findByOwnerOrderByCreatedAtDesc(owner);
        }
        return repository.findByOwnerAndDoneOrderByCreatedAtDesc(owner, false);
    }

    @PostMapping
    public Task create(@RequestBody TaskRequest request, Principal principal) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDone(false);
        task.setOwner(currentUser(principal));
        return repository.save(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @RequestBody TaskRequest request, Principal principal) {
        return repository.findByIdAndOwner(id, currentUser(principal))
                .map(existing -> {
                    if (request.title() != null) {
                        existing.setTitle(request.title());
                    }
                    if (request.description() != null) {
                        existing.setDescription(request.description());
                    }
                    if (request.done() != null) {
                        existing.setDone(request.done());
                    }
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        return repository.findByIdAndOwner(id, currentUser(principal))
                .map(task -> {
                    repository.delete(task);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private User currentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException(principal.getName()));
    }

    public record TaskRequest(String title, String description, Boolean done) {
    }
}
