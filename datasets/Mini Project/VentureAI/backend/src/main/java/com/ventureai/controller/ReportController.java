package com.ventureai.controller;

import com.ventureai.entity.Report;
import com.ventureai.service.ReportService;
import com.ventureai.util.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{validationId}")
    public ResponseEntity<byte[]> viewInline(@PathVariable Long validationId) throws Exception {
        return serve(validationId, false);
    }

    @GetMapping("/{validationId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long validationId) throws Exception {
        return serve(validationId, true);
    }

    private ResponseEntity<byte[]> serve(Long validationId, boolean forceDownload) throws Exception {
        Report report = reportService.getOrGenerateReport(currentUserProvider.getCurrentUser(), validationId);
        File file = reportService.resolveFile(report);
        byte[] bytes = new FileSystemResource(file).getInputStream().readAllBytes();

        ContentDisposition disposition = (forceDownload ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("VentureAI-Validation-Report-" + validationId + ".pdf")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
