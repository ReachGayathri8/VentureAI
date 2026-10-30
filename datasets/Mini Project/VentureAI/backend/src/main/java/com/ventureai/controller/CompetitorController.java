package com.ventureai.controller;

import com.ventureai.dto.startup.CompetitorDto;
import com.ventureai.dto.startup.CompetitorRequest;
import com.ventureai.entity.Competitor;
import com.ventureai.entity.Startup;
import com.ventureai.entity.User;
import com.ventureai.entity.enums.CompetitorSource;
import com.ventureai.repository.CompetitorRepository;
import com.ventureai.service.StartupService;
import com.ventureai.service.ai.GeminiAiService;
import com.ventureai.util.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/startups/{startupId}/competitors")
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorRepository competitorRepository;
    private final StartupService startupService;
    private final GeminiAiService geminiAiService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<CompetitorDto>> list(@PathVariable Long startupId) {
        Startup s = startupService.getOwnedStartup(currentUserProvider.getCurrentUser(), startupId);
        return ResponseEntity.ok(competitorRepository.findByStartup(s).stream().map(this::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<CompetitorDto> add(@PathVariable Long startupId, @Valid @RequestBody CompetitorRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Startup s = startupService.getOwnedStartup(user, startupId);
        Competitor c = Competitor.builder()
                .startup(s).name(request.name()).website(request.website()).description(request.description())
                .source(CompetitorSource.MANUAL)
                .priceRating(3).featureRating(3).marketReachRating(3).innovationRating(3)
                .build();
        return ResponseEntity.ok(toDto(competitorRepository.save(c)));
    }

    @PostMapping("/discover")
    public ResponseEntity<List<CompetitorDto>> discover(@PathVariable Long startupId) {
        User user = currentUserProvider.getCurrentUser();
        Startup s = startupService.getOwnedStartup(user, startupId);
        List<Competitor> discovered = geminiAiService.discoverCompetitors(s, 3);
        competitorRepository.saveAll(discovered);
        return ResponseEntity.ok(discovered.stream().map(this::toDto).toList());
    }

    @DeleteMapping("/{competitorId}")
    public ResponseEntity<Void> delete(@PathVariable Long startupId, @PathVariable Long competitorId) {
        User user = currentUserProvider.getCurrentUser();
        startupService.getOwnedStartup(user, startupId); // ownership check
        competitorRepository.deleteById(competitorId);
        return ResponseEntity.noContent().build();
    }

    private CompetitorDto toDto(Competitor c) {
        return new CompetitorDto(c.getId(), c.getName(), c.getWebsite(), c.getDescription(), c.getPricing(),
                c.getStrengths(), c.getWeaknesses(), c.getMarketPosition(), c.getSource().name(),
                c.getPriceRating(), c.getFeatureRating(), c.getMarketReachRating(), c.getInnovationRating());
    }
}
