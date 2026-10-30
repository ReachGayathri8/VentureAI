package com.ventureai.service;

import com.ventureai.entity.Report;
import com.ventureai.entity.User;
import com.ventureai.entity.Validation;
import com.ventureai.repository.ReportRepository;
import com.ventureai.service.report.ReportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportPdfService reportPdfService;
    private final ValidationService validationService;

    @Transactional
    public Report getOrGenerateReport(User user, Long validationId) {
        Validation validation = validationService.getOwnedValidationEntity(user, validationId);
        return reportRepository.findByValidation(validation).orElseGet(() -> {
            try {
                return reportPdfService.generateReport(validation);
            } catch (Exception e) {
                throw new RuntimeException("Unable to generate PDF report. Please try again.", e);
            }
        });
    }

    public File resolveFile(Report report) {
        return reportPdfService.getReportFile(report);
    }
}
