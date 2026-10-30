package com.ventureai.controller;

import com.ventureai.dto.common.MessageResponse;
import com.ventureai.dto.startup.StartupRequest;
import com.ventureai.dto.startup.StartupResponse;
import com.ventureai.entity.Startup;
import com.ventureai.entity.User;
import com.ventureai.service.StartupService;
import com.ventureai.util.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/startups")
@RequiredArgsConstructor
public class StartupController {

    private final StartupService startupService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<StartupResponse> create(@Valid @RequestBody StartupRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Startup startup = startupService.createStartup(user, request);
        return ResponseEntity.ok(startupService.getStartupResponse(user, startup.getId()));
    }

    @GetMapping
    public ResponseEntity<List<StartupResponse>> list() {
        return ResponseEntity.ok(startupService.listStartups(currentUserProvider.getCurrentUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StartupResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(startupService.getStartupResponse(currentUserProvider.getCurrentUser(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StartupResponse> update(@PathVariable Long id, @Valid @RequestBody StartupRequest request) {
        return ResponseEntity.ok(startupService.updateStartup(currentUserProvider.getCurrentUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        startupService.deleteStartup(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.ok(new MessageResponse("Startup deleted."));
    }
}
