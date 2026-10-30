package com.ventureai.service.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ventureai.config.ReportsProperties;
import com.ventureai.dto.ai.MarketOpportunityResponse;
import com.ventureai.dto.ai.RecommendationsResponse;
import com.ventureai.dto.ai.RiskAnalysisResponse;
import com.ventureai.dto.ai.RiskCategoryDto;
import com.ventureai.entity.*;
import com.ventureai.exception.ResourceNotFoundException;
import com.ventureai.repository.*;
import com.ventureai.service.finance.FinanceEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates the downloadable "AI Startup Validation Report" PDF (spec
 * section 23) covering every required section: executive summary, startup
 * overview, viability score + breakdown, NLP analysis, market analysis,
 * competitor intelligence, SWOT, financial analysis (P&L/Cash Flow/Balance
 * Sheet/Break-even/Revenue Projection), risk analysis, AI recommendations
 * and final verdict.
 */
@Service
@RequiredArgsConstructor
public class ReportPdfService {

    private final ReportsProperties reportsProperties;
    private final ReportRepository reportRepository;
    private final SwotAnalysisRepository swotAnalysisRepository;
    private final AiInsightRepository aiInsightRepository;
    private final CompetitorRepository competitorRepository;
    private final FinancialDataRepository financialDataRepository;
    private final FinancialProjectionRepository financialProjectionRepository;
    private final FinanceEngineService financeEngineService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Color NAVY = new Color(19, 32, 71);
    private static final Color RED_ACCENT = new Color(200, 30, 45);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, NAVY);
    private static final Font H2_FONT = new Font(Font.HELVETICA, 15, Font.BOLD, NAVY);
    private static final Font H3_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, RED_ACCENT);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);

    @Transactional
    public Report generateReport(Validation validation) throws Exception {
        Startup startup = validation.getStartup();
        SwotAnalysis swot = swotAnalysisRepository.findByValidation(validation)
                .orElseThrow(() -> new ResourceNotFoundException("SWOT missing for validation."));
        AiInsight insight = aiInsightRepository.findByValidation(validation)
                .orElseThrow(() -> new ResourceNotFoundException("AI insight missing for validation."));
        List<Competitor> competitors = competitorRepository.findByStartupId(startup.getId());
        FinancialData fd = financialDataRepository.findByStartup(startup).orElse(null);
        List<FinancialProjection> projections = financialProjectionRepository.findByStartupOrderByPeriodAsc(startup);

        File dir = new File(reportsProperties.getStorageDir());
        if (!dir.exists()) dir.mkdirs();
        String fileName = "validation_" + validation.getId() + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(dir, fileName);

        Document document = new Document(PageSize.A4, 50, 50, 60, 50);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        addCoverSection(document, startup, validation);
        addExecutiveSummary(document, startup, validation, insight);
        addStartupOverview(document, startup);
        addViabilityScore(document, validation);
        addNlpAnalysis(document, validation);
        addMarketAnalysis(document, insight);
        addCompetitorIntelligence(document, competitors, insight);
        addSwot(document, swot);
        if (fd != null && !projections.isEmpty()) {
            addFinancialAnalysis(document, fd, projections);
        }
        addRiskAnalysis(document, insight);
        addRecommendations(document, insight);
        addFinalVerdict(document, startup, validation, insight);

        document.close();

        Report report = Report.builder()
                .validation(validation)
                .filePath(file.getAbsolutePath())
                .build();
        return reportRepository.save(report);
    }

    public File getReportFile(Report report) {
        File f = new File(report.getFilePath());
        if (!f.exists()) throw new ResourceNotFoundException("Report file no longer exists on disk.");
        return f;
    }

    // ---------------- sections ----------------

    private void addCoverSection(Document doc, Startup s, Validation v) throws DocumentException {
        Paragraph title = new Paragraph("AI Startup Validation Report", TITLE_FONT);
        title.setSpacingAfter(4);
        doc.add(title);
        Paragraph tagline = new Paragraph("VentureAI — Validate your idea. Understand your market. Build with confidence.", SMALL_FONT);
        tagline.setSpacingAfter(10);
        doc.add(tagline);

        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        addKeyValueRow(meta, "Startup", s.getName());
        addKeyValueRow(meta, "Industry", s.getIndustry());
        addKeyValueRow(meta, "Generated on", v.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        doc.add(meta);
        addSpacer(doc);
    }

    private void addExecutiveSummary(Document doc, Startup s, Validation v, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "1. Executive Summary");
        doc.add(new Paragraph(String.format(
                "%s is a %s venture with an overall viability score of %.0f/100 (%s, %.0f%% confidence). %s",
                s.getName(), s.getIndustry(), v.getViabilityScore(), v.getClassification().display(),
                v.getConfidence() * 100, insight.getFinalVerdict()), BODY_FONT));
        addSpacer(doc);
    }

    private void addStartupOverview(Document doc, Startup s) throws DocumentException {
        addSectionHeader(doc, "2. Startup Overview");
        addLabeledParagraph(doc, "Problem", s.getProblem());
        addLabeledParagraph(doc, "Solution", s.getSolution());
        addLabeledParagraph(doc, "Target Market", s.getTargetMarket());
        addLabeledParagraph(doc, "Target Customers", s.getTargetCustomers());
        addLabeledParagraph(doc, "Business Model", s.getBusinessModel());
        addLabeledParagraph(doc, "Revenue Model", s.getRevenueModel());
        addLabeledParagraph(doc, "Unique Value Proposition", s.getUniqueValueProposition());
        addLabeledParagraph(doc, "Marketing Strategy", s.getMarketingStrategy());
        addSpacer(doc);
    }

    private void addViabilityScore(Document doc, Validation v) throws DocumentException {
        addSectionHeader(doc, "3. Viability Score & 4. Score Breakdown");
        doc.add(new Paragraph(String.format("Overall Viability: %.0f / 100  —  %s (confidence %.0f%%)",
                v.getViabilityScore(), v.getClassification().display(), v.getConfidence() * 100), H3_FONT));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        addKeyValueRow(table, "Market Potential", round(v.getMarketScore()) + " / 100");
        addKeyValueRow(table, "Business Model", round(v.getBusinessModelScore()) + " / 100");
        addKeyValueRow(table, "Competition (advantage)", round(100 - v.getCompetitionScore()) + " / 100");
        addKeyValueRow(table, "Financial Feasibility", round(v.getFinancialScore()) + " / 100");
        addKeyValueRow(table, "Scalability", round(v.getScalabilityScore()) + " / 100");
        addKeyValueRow(table, "Risk Level", v.getRiskLevel().name());
        doc.add(table);
        addSpacer(doc);
    }

    private void addNlpAnalysis(Document doc, Validation v) throws DocumentException {
        addSectionHeader(doc, "5. NLP Analysis");
        doc.add(new Paragraph("Industry / Category Used by Validation: " + v.getDetectedIndustry(), BODY_FONT));
        try {
            List<String> keywords = objectMapper.readValue(v.getExtractedKeywords(), new TypeReference<List<String>>() {});
            doc.add(new Paragraph("Extracted Keywords: " + String.join(", ", keywords), BODY_FONT));
        } catch (Exception ignored) { }
        addSpacer(doc);
    }

    private void addMarketAnalysis(Document doc, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "6. Market Analysis");
        try {
            MarketOpportunityResponse mo = objectMapper.readValue(insight.getMarketOpportunitiesJson(), MarketOpportunityResponse.class);
            doc.add(new Paragraph("Target Market: " + mo.targetMarket(), BODY_FONT));
            doc.add(new Paragraph("Market Demand: " + mo.marketDemand(), BODY_FONT));
            doc.add(new Paragraph("Growth Potential: " + mo.growthPotential(), BODY_FONT));
            addBulletList(doc, "Customer Segments", mo.customerSegments());
            addBulletList(doc, "Market Trends", mo.marketTrends());
            addBulletList(doc, "Opportunity Areas", mo.opportunityAreas());
            addBulletList(doc, "Major Risks", mo.majorRisks());
            if (mo.aiEstimated()) {
                doc.add(new Paragraph("Note: figures above are AI-generated estimates (no external market-data feed connected).", SMALL_FONT));
            }
        } catch (Exception ignored) { }
        addSpacer(doc);
    }

    private void addCompetitorIntelligence(Document doc, List<Competitor> competitors, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "7. Competitor Intelligence");
        if (competitors.isEmpty()) {
            doc.add(new Paragraph("No competitors were supplied or discovered for this validation.", BODY_FONT));
        } else {
            PdfPTable table = new PdfPTable(new float[]{1.8f, 2.6f, 1.2f, 1.8f, 1.8f, 2.0f});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Competitor");
            addHeaderCell(table, "Description");
            addHeaderCell(table, "Pricing");
            addHeaderCell(table, "Strengths");
            addHeaderCell(table, "Weaknesses");
            addHeaderCell(table, "Market Position");
            for (Competitor c : competitors) {
                table.addCell(new PdfPCell(new Phrase(safe(c.getName()), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(safe(c.getDescription()), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(safe(c.getPricing()), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(safe(c.getStrengths()), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(safe(c.getWeaknesses()), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(safe(c.getMarketPosition()), BODY_FONT)));
            }
            doc.add(table);
        }
        doc.add(new Paragraph("Competitive Advantage: " + safe(insight.getCompetitiveAdvantage()), BODY_FONT));
        doc.add(new Paragraph("Market Gap: " + safe(insight.getMarketGap()), BODY_FONT));
        doc.add(new Paragraph("Recommended Differentiation: " + safe(insight.getDifferentiationStrategy()), BODY_FONT));
        addSpacer(doc);
    }

    private void addSwot(Document doc, SwotAnalysis swot) throws DocumentException {
        addSectionHeader(doc, "8. SWOT Analysis");
        addBulletListFromJson(doc, "Strengths", swot.getStrengths());
        addBulletListFromJson(doc, "Weaknesses", swot.getWeaknesses());
        addBulletListFromJson(doc, "Opportunities", swot.getOpportunities());
        addBulletListFromJson(doc, "Threats", swot.getThreats());
        addSpacer(doc);
    }

    private void addFinancialAnalysis(Document doc, FinancialData fd, List<FinancialProjection> rows) throws DocumentException {
        addSectionHeader(doc, "9-13. Financial Analysis, Revenue Projection, P&L, Cash Flow, Balance Sheet, Break-even");
        var summary = financeEngineService.summarize(fd, rows);
        var breakEven = financeEngineService.breakEven(fd);

        // Year 1 snapshot = Month 12 (or the last available month if the
        // projection is shorter than a year) - never the end of the full
        // multi-year projection. See maxProjectedFinancingRequirement
        // below for the full-period figure.
        int year1SnapshotPeriod = Math.min(12, rows.size());
        var balanceSheet = financeEngineService.balanceSheet(fd, rows, year1SnapshotPeriod);
        var maxFinancingRequirement = financeEngineService.maxProjectedFinancingRequirement(rows);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        addKeyValueRow(summaryTable, "Monthly Revenue (Month 1)", currency(summary.monthlyRevenue()));
        addKeyValueRow(summaryTable, "Annual Revenue (Year 1)", currency(summary.annualRevenue()));
        addKeyValueRow(summaryTable, "Monthly Operating Expenses", currency(summary.monthlyOperatingExpenses()));
        addKeyValueRow(summaryTable, "Net Profit (Month 1)", currency(summary.netProfitMonth1()));
        addKeyValueRow(summaryTable, "Year 1 Profit Margin", summary.profitMarginPercent() + "%");
        addKeyValueRow(summaryTable, "ROI (Year 1)", summary.roiPercent() + "%");
        addKeyValueRow(summaryTable, "Average Monthly Operating Burn", currency(summary.burnRate()));
        addKeyValueRow(summaryTable, "Projected Cash Runway", summary.runwayMonths() == null ? "Cash never projected to reach zero within this plan" : String.format("%.1f months", summary.runwayMonths()));
        doc.add(summaryTable);

        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph("Break-even Analysis", H3_FONT));
        doc.add(new Paragraph(String.format("Fixed Costs: %s | Variable Cost/Unit: %s | Selling Price/Unit: %s | Contribution Margin: %s",
                currency(breakEven.fixedCosts()), currency(breakEven.variableCostPerUnit()),
                currency(breakEven.sellingPricePerUnit()), currency(breakEven.contributionMargin())), BODY_FONT));
        doc.add(new Paragraph(breakEven.breakEvenUnits() == null
                ? "Break-even point could not be computed (selling price must exceed variable cost)."
                : String.format("Break-even Units: %.0f  |  Break-even Revenue: %s", breakEven.breakEvenUnits(), currency(breakEven.breakEvenRevenue())), BODY_FONT));

        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph("Balance Sheet (Year 1 snapshot — Month " + year1SnapshotPeriod + ")", H3_FONT));
        PdfPTable bsTable = new PdfPTable(2);
        bsTable.setWidthPercentage(100);
        addKeyValueRow(bsTable, "Cash", currency(balanceSheet.cash()));
        addKeyValueRow(bsTable, "Accounts Receivable", currency(balanceSheet.accountsReceivable()));
        addKeyValueRow(bsTable, "Inventory", currency(balanceSheet.inventory()));
        addKeyValueRow(bsTable, "Net Fixed Assets", currency(balanceSheet.netFixedAssets()));
        addKeyValueRow(bsTable, "Total Assets", currency(balanceSheet.totalAssets()));
        addKeyValueRow(bsTable, "Existing Debt", currency(balanceSheet.existingDebt()));
        addKeyValueRow(bsTable, "Projected Funding Gap (Year 1)", currency(balanceSheet.projectedFundingGap()));
        addKeyValueRow(bsTable, "Accounts Payable", currency(balanceSheet.accountsPayable()));
        addKeyValueRow(bsTable, "Founder Capital", currency(balanceSheet.founderCapital()));
        addKeyValueRow(bsTable, "Retained Earnings", currency(balanceSheet.retainedEarnings()));
        addKeyValueRow(bsTable, "Total Liabilities", currency(balanceSheet.totalLiabilities()));
        addKeyValueRow(bsTable, "Total Liabilities + Equity", currency(balanceSheet.totalLiabilitiesAndEquity()));
        addKeyValueRow(bsTable, "Balance Check", currency(balanceSheet.balanceCheck()));
        doc.add(bsTable);

        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph(String.format(
                "Maximum Projected Financing Requirement (worst point across the full %d-month projection): %s. "
                        + "This is a separate, later-in-time figure from the Year 1 snapshot above and is not "
                        + "itself part of the Year 1 balance check.",
                rows.size(), currency(maxFinancingRequirement)), SMALL_FONT));

        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph("Monthly P&L (first 12 months)", H3_FONT));
        PdfPTable pnl = new PdfPTable(5);
        pnl.setWidthPercentage(100);
        addHeaderCell(pnl, "Month"); addHeaderCell(pnl, "Revenue"); addHeaderCell(pnl, "COGS");
        addHeaderCell(pnl, "Gross Profit"); addHeaderCell(pnl, "Net Profit");
        rows.stream().limit(12).forEach(r -> {
            pnl.addCell(new PdfPCell(new Phrase(String.valueOf(r.getPeriod()), BODY_FONT)));
            pnl.addCell(new PdfPCell(new Phrase(currency(r.getRevenue()), BODY_FONT)));
            pnl.addCell(new PdfPCell(new Phrase(currency(r.getCogs()), BODY_FONT)));
            pnl.addCell(new PdfPCell(new Phrase(currency(r.getGrossProfit()), BODY_FONT)));
            pnl.addCell(new PdfPCell(new Phrase(currency(r.getNetProfit()), BODY_FONT)));
        });
        doc.add(pnl);
        addSpacer(doc);
    }

    private void addRiskAnalysis(Document doc, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "14. Risk Analysis");
        try {
            RiskAnalysisResponse risk = objectMapper.readValue(insight.getRisksJson(), RiskAnalysisResponse.class);
            doc.add(new Paragraph("Overall Risk Score: " + risk.overallRiskScore() + " (" + risk.overallRiskLevel() + ")", H3_FONT));
            PdfPTable table = new PdfPTable(new float[]{2, 1, 1, 3});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Category"); addHeaderCell(table, "Risk"); addHeaderCell(table, "Impact"); addHeaderCell(table, "Mitigation");
            for (RiskCategoryDto c : risk.categories()) {
                table.addCell(new PdfPCell(new Phrase(c.category(), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(c.riskLevel(), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(c.impact(), BODY_FONT)));
                table.addCell(new PdfPCell(new Phrase(c.mitigationStrategy(), BODY_FONT)));
            }
            doc.add(table);
        } catch (Exception ignored) { }
        addSpacer(doc);
    }

    private void addRecommendations(Document doc, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "15. AI Recommendations");
        try {
            RecommendationsResponse rec = objectMapper.readValue(insight.getRecommendationsJson(), RecommendationsResponse.class);
            doc.add(new Paragraph(rec.summary(), BODY_FONT));
            addBulletList(doc, "Business", rec.business());
            addBulletList(doc, "Marketing", rec.marketing());
            addBulletList(doc, "Pricing", rec.pricing());
            addBulletList(doc, "Revenue Model", rec.revenueModel());
            addBulletList(doc, "Product", rec.product());
            addBulletList(doc, "Growth Strategy", rec.growthStrategy());
            addBulletList(doc, "Risk Mitigation", rec.riskMitigation());
            addBulletList(doc, "Funding", rec.funding());
        } catch (Exception ignored) { }
        addSpacer(doc);
    }

    private void addFinalVerdict(Document doc, Startup s, Validation v, AiInsight insight) throws DocumentException {
        addSectionHeader(doc, "16. Final Verdict");
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addKeyValueRow(table, "Overall Score", round(v.getViabilityScore()) + " / 100");
        addKeyValueRow(table, "Classification", v.getClassification().display());
        doc.add(table);
        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph(insight.getFinalVerdict(), H3_FONT));
        doc.add(new Paragraph(" ", BODY_FONT));
        doc.add(new Paragraph("Recommended Next Steps:", H3_FONT));
        doc.add(new Paragraph("1. Validate the core problem/solution fit with 10-20 real target customers.", BODY_FONT));
        doc.add(new Paragraph("2. Run a small pilot to test the pricing and revenue model.", BODY_FONT));
        doc.add(new Paragraph("3. Track burn rate and runway monthly against this financial plan.", BODY_FONT));
        doc.add(new Paragraph("4. Revisit this validation after the pilot to see the score improve.", BODY_FONT));
    }

    // ---------------- helpers ----------------

    private void addSectionHeader(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, H2_FONT);
        p.setSpacingBefore(8);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void addSpacer(Document doc) throws DocumentException {
        doc.add(new Paragraph(" ", SMALL_FONT));
    }

    private void addLabeledParagraph(Document doc, String label, String value) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", H3_FONT));
        p.add(new Chunk(safe(value), BODY_FONT));
        p.setSpacingAfter(3);
        doc.add(p);
    }

    private void addBulletList(Document doc, String label, List<String> items) throws DocumentException {
        if (items == null || items.isEmpty()) return;
        doc.add(new Paragraph(label + ":", H3_FONT));
        com.lowagie.text.List pdfList = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
        for (String item : items) pdfList.add(new ListItem(item, BODY_FONT));
        doc.add(pdfList);
    }

    private void addBulletListFromJson(Document doc, String label, String json) throws DocumentException {
        try {
            List<String> items = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            addBulletList(doc, label, items);
        } catch (Exception ignored) { }
    }

    private void addKeyValueRow(PdfPTable table, String key, String value) {
        PdfPCell k = new PdfPCell(new Phrase(key, H3_FONT));
        k.setBorderColor(Color.LIGHT_GRAY);
        k.setPadding(5);
        PdfPCell v = new PdfPCell(new Phrase(safe(value), BODY_FONT));
        v.setBorderColor(Color.LIGHT_GRAY);
        v.setPadding(5);
        table.addCell(k);
        table.addCell(v);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(NAVY);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String currency(BigDecimal value) {
        if (value == null) return "-";
        return "Rs. " + String.format("%,.2f", value);
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
