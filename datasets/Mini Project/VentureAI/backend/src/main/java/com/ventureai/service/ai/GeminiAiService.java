package com.ventureai.service.ai;



import com.fasterxml.jackson.databind.JsonNode;

import com.ventureai.dto.ai.*;

import com.ventureai.entity.Competitor;

import com.ventureai.entity.Startup;

import com.ventureai.entity.enums.CompetitorSource;

import com.ventureai.service.ml.NlpExtractionResult;

import com.ventureai.service.ml.generative.GenerativeAiClient;

import com.ventureai.service.ml.MlPredictionResult;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;



import java.net.URI;

import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Set;

import java.util.HashSet;

import java.util.stream.StreamSupport;



/**

 * AI Business Intelligence Engine.

 *

 * Gemini is used for qualitative business intelligence:

 * - SWOT

 * - Competitive positioning

 * - Market gaps

 * - Differentiation

 * - Customer segments

 * - Market trends

 * - Opportunities

 * - Risks

 * - Recommendations

 *

 * Important:

 * Gemini must not invent numeric market facts such as:

 * - TAM

 * - SAM

 * - SOM

 * - Market share

 * - Industry CAGR

 * - Competitor revenue

 * - Competitor market share

 *

 * Numeric indicators supplied by VentureAI are clearly labelled

 * as model estimates or founder assumptions.

 */

@Service

@RequiredArgsConstructor

@Slf4j

public class GeminiAiService {



    private final GeminiClient geminiClient;

    private final RuleBasedAnalysisEngine ruleBasedAnalysisEngine;

    private final TavilyClient tavilyClient;

    private final GenerativeAiClient generativeAiClient;



    // ============================================================

    // FULL AI ANALYSIS

    // ============================================================



    public AiAnalysisBundle generateFullAnalysis(

            Startup startup,

            List<Competitor> competitors,

            NlpExtractionResult nlp,

            MlPredictionResult prediction,

            double marketSize,

            double competitionLevel,

            double growthRate

    ) {



        if (geminiClient.isEnabled()) {

            try {

                AiAnalysisBundle geminiResult = callGeminiForFullAnalysis(

                        startup, competitors, nlp, prediction,

                        marketSize, competitionLevel, growthRate

                );

                if (geminiResult != null) {

                    log.info("Gemini full-analysis completed successfully.");

                    return geminiResult;

                }

            } catch (Exception ex) {

                log.warn("Gemini full-analysis failed. Starting Qwen + Flan-T5 fallback: {}", ex.getMessage());

            }

        } else {

            log.warn("Gemini is disabled. Starting Qwen + Flan-T5 fallback.");

        }



        try {

            String fallbackPrompt = buildFallbackPrompt(

                    startup, competitors, nlp, prediction,

                    marketSize, competitionLevel, growthRate

            );

            String qwenResult = generativeAiClient.generateQwen(fallbackPrompt);

            String flanResult = generativeAiClient.generateFlanT5(fallbackPrompt);

            log.info("Qwen fallback completed: {} characters", qwenResult != null ? qwenResult.length() : 0);

            log.info("Flan-T5 fallback completed: {} characters", flanResult != null ? flanResult.length() : 0);

        } catch (Exception fallbackEx) {

            log.warn("Qwen/Flan-T5 fallback failed: {}", fallbackEx.getMessage());

        }



        return ruleBasedAnalysisEngine.analyze(

                startup, competitors, nlp, prediction,

                marketSize, competitionLevel, growthRate

        );

    }



    private String buildFallbackPrompt(

            Startup startup,

            List<Competitor> competitors,

            NlpExtractionResult nlp,

            MlPredictionResult prediction,

            double marketSize,

            double competitionLevel,

            double growthRate

    ) {

        return "Analyze this startup for validation. "

                + "Startup Name: " + startup.getName()

                + ". Industry: " + startup.getIndustry()

                + ". BART Classification: " + prediction.classification()

                + ". BART Score: " + prediction.score()

                + ". Market Size Indicator: " + marketSize

                + ". Competition Level: " + competitionLevel

                + ". Growth Rate: " + growthRate

                + ". Provide concise startup validation insights, market opportunities, "

                + "competitive considerations, risks, strengths, weaknesses and actionable recommendations.";

    }



    // ============================================================

    // COMPETITOR DISCOVERY

    // ============================================================



    /**

     * Automatic competitor discovery uses Tavily for live web search.

     *

     * Important: this method never creates heuristic or placeholder

     * competitors. If the web search does not return credible results,

     * an empty list is returned.

     */

    public List<Competitor> discoverCompetitors(

            Startup startup,

            int count

    ) {

        int requested = Math.max(1, Math.min(count, 10));



        if (!tavilyClient.isEnabled()) {

            log.warn("Automatic competitor discovery skipped because Tavily is not configured.");

            return List.of();

        }



        try {

            return discoverCompetitorsWithTavily(startup, requested);

        } catch (Exception ex) {

            log.warn("Tavily competitor discovery failed: {}", ex.getMessage());

            return List.of();

        }

    }



    private List<Competitor> discoverCompetitorsWithTavily(

            Startup s,

            int count

    ) {

        List<String> keywords = extractDomainKeywords(s);



        String query1 = buildCompetitorQuery(s, keywords);

        String query2 = buildAlternativeCompetitorQuery(s, keywords);



        List<JsonNode> searchResults = new ArrayList<>();

        collectTavilyResults(searchResults, tavilyClient.search(query1, 8));

        collectTavilyResults(searchResults, tavilyClient.search(query2, 8));



        List<CompetitorCandidate> candidates = new ArrayList<>();



        for (JsonNode result : searchResults) {

            String title = safe(result.path("title").asText(""));

            String url = safe(result.path("url").asText(""));

            String content = safe(result.path("content").asText(""));



            if (!isCredibleCompetitorResult(title, url, content, s, keywords)) {

                continue;

            }



            String domain = normalizeDomain(url);

            if (domain.isBlank()) {

                continue;

            }



            String name = cleanCompetitorName(title, domain);

            if (isGenericCompetitorName(name, keywords, domain)) {

                name = cleanCompetitorNameFromDomain(domain);

            }



            if (name.isBlank() || isGenericCompetitorName(name, keywords, domain)) {

                continue;

            }



            int relevance = competitorRelevanceScore(s, title, content, domain, keywords);

            if (relevance < 5) {

                continue;

            }



            candidates.add(new CompetitorCandidate(

                    domain,

                    name,

                    url,

                    limitText(content, 700),

                    relevance,

                    result

            ));

        }



        candidates.sort((a, b) -> Integer.compare(b.relevance, a.relevance));



        Map<String, CompetitorCandidate> unique = new LinkedHashMap<>();

        for (CompetitorCandidate candidate : candidates) {

            if (unique.size() >= count) {

                break;

            }

            unique.putIfAbsent(candidate.domain, candidate);

        }



        List<Competitor> competitors = new ArrayList<>();

        List<JsonNode> selectedEvidence = new ArrayList<>();



        for (CompetitorCandidate candidate : unique.values()) {

            Competitor competitor = Competitor.builder()

                    .startup(s)

                    .name(candidate.name)

                    .website(candidate.url)

                    .description(candidate.description)

                    .pricing("Not publicly available")

                    .strengths("Not enough public evidence found")

                    .weaknesses("Not enough public evidence found")

                    .marketPosition("Not enough public evidence found")

                    .source(CompetitorSource.AUTO_DISCOVERED)

                    .priceRating(3)

                    .featureRating(3)

                    .marketReachRating(3)

                    .innovationRating(3)

                    .build();



            competitors.add(competitor);

            selectedEvidence.add(candidate.result);

        }



        enrichCompetitorsFromTavilyEvidence(competitors, s, selectedEvidence);

        return competitors;

    }



    private record CompetitorCandidate(

            String domain,

            String name,

            String url,

            String description,

            int relevance,

            JsonNode result

    ) {}



    private int competitorRelevanceScore(

            Startup s,

            String title,

            String content,

            String domain,

            List<String> keywords

    ) {

        String text = (safe(title) + " " + safe(content) + " " + safe(domain))

                .toLowerCase(Locale.ROOT);



        int score = 0;



        int keywordMatches = 0;

        for (String keyword : keywords) {

            if (!keyword.isBlank() && text.contains(keyword)) {

                keywordMatches++;

            }

        }

        score += Math.min(4, keywordMatches * 2);



        String location = safe(s.getLocation()).toLowerCase(Locale.ROOT);

        if (!location.isBlank()) {

            for (String term : location.replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {

                if (term.length() >= 4 && text.contains(term)) {

                    score += 2;

                    break;

                }

            }

        }



        if (looksLikeFirstPartyBusinessPage(domain, title, content)) score += 2;

        return score;

    }



    private boolean looksLikeFirstPartyBusinessPage(

            String domain,

            String title,

            String content

    ) {

        if (domain.isBlank()) return false;

        String lowerTitle = safe(title).toLowerCase(Locale.ROOT);

        String lowerContent = safe(content).toLowerCase(Locale.ROOT);

        return !lowerTitle.contains("top ")

                && !lowerTitle.contains("best ")

                && !lowerTitle.contains("list of")

                && !lowerTitle.contains("alternatives to")

                && !lowerTitle.contains("competitors of")

                && !lowerTitle.contains("comparison")

                && !lowerContent.contains("list of competitors")

                && !lowerContent.contains("top 10")

                && !lowerContent.contains("top 20");

    }



    /**

     * Enriches all discovered competitors in ONE Gemini request.

     * Tavily supplies the live evidence; Gemini only summarizes that evidence.

     * This avoids one Gemini request per competitor and reduces rate-limit risk.

     */

    private void enrichCompetitorsFromTavilyEvidence(

            List<Competitor> competitors,

            Startup startup,

            List<JsonNode> searchResults

    ) {

        if (!geminiClient.isEnabled() || competitors.isEmpty()) {

            return;

        }



        try {

            Map<String, JsonNode> evidenceByDomain = new LinkedHashMap<>();

            for (JsonNode result : searchResults) {

                String url = safe(result.path("url").asText(""));

                String domain = normalizeDomain(url);

                if (!domain.isBlank() && !evidenceByDomain.containsKey(domain)) {

                    evidenceByDomain.put(domain, result);

                }

            }



            StringBuilder evidenceBlock = new StringBuilder();

            for (int i = 0; i < competitors.size(); i++) {

                Competitor competitor = competitors.get(i);

                String domain = normalizeDomain(competitor.getWebsite());

                JsonNode result = evidenceByDomain.get(domain);



                evidenceBlock

                        .append("COMPETITOR_INDEX: ")

                        .append(i)

                        .append("\nName: ")

                        .append(safe(competitor.getName()))

                        .append("\nWebsite: ")

                        .append(safe(competitor.getWebsite()))

                        .append("\nSearch title: ")

                        .append(result == null ? "" : safe(result.path("title").asText("")))

                        .append("\nTavily evidence: ")

                        .append(result == null ? "" : limitText(result.path("content").asText(""), 5000))

                        .append("\n\n");

            }



            String system = """

                    You are VentureAI's evidence-based competitor analyst.



                    Return ONLY valid JSON. No markdown.

                    Use exactly this structure:

                    {

                      "competitors": [

                        {

                          "index": number,

                          "pricing": string,

                          "strengths": string,

                          "weaknesses": string,

                          "marketPosition": string

                        }

                      ]

                    }



                    STRICT EVIDENCE RULES:

                    1. Use ONLY the supplied Tavily evidence for each competitor.

                    2. Do NOT use general knowledge or memory.

                    3. Do NOT invent prices, plans, discounts, features, customers,

                       market share, revenue, rankings or geographic coverage.

                    4. Pricing must contain only pricing explicitly present in the

                       evidence. If no pricing is present, return exactly:

                       "Not publicly available".

                    5. Strengths must be directly supported by the evidence.

                       If insufficient, return exactly:

                       "Not enough public evidence found".

                    6. Weaknesses must be directly supported by the evidence.

                       Do not infer a weakness from missing information. If none is

                       supported, return exactly:

                       "Not enough public evidence found".

                    7. Market position must describe only what the evidence supports,

                       such as product focus, customer focus, service model or

                       geographic focus. Do not claim market leadership unless the

                       evidence explicitly supports it.

                    8. Keep every field concise, factual and startup-relevant.

                    9. Never fabricate missing information.

                    """;



            String prompt = """

                    STARTUP CONTEXT

                    Startup: %s

                    Industry: %s

                    Problem: %s

                    Solution: %s

                    Target market: %s



                    TAVILY COMPETITOR EVIDENCE

                    %s



                    For every competitor, return the same COMPETITOR_INDEX supplied

                    above. Analyze each competitor using ONLY its Tavily evidence.

                    """.formatted(

                    safe(startup.getName()),

                    safe(startup.getIndustry()),

                    safe(startup.getProblem()),

                    safe(startup.getSolution()),

                    safe(startup.getTargetMarket()),

                    evidenceBlock

            );



            JsonNode root = geminiClient.generateJson(system, prompt);

            JsonNode nodes = root.path("competitors");



            if (!nodes.isArray()) {

                return;

            }



            nodes.forEach(node -> {

                int index = node.path("index").asInt(-1);

                if (index < 0 || index >= competitors.size()) {

                    return;

                }



                Competitor competitor = competitors.get(index);

                competitor.setPricing(

                        normalizePricing(node.path("pricing").asText(""))

                );

                competitor.setStrengths(

                        normalizeAnalysisField(node.path("strengths").asText(""))

                );

                competitor.setWeaknesses(

                        normalizeAnalysisField(node.path("weaknesses").asText(""))

                );

                String marketPosition =

                        normalizeAnalysisField(

                                node.path("marketPosition").asText("")

                        );

                competitor.setMarketPosition(

                        limitText(marketPosition, 150)

                );

            });



        } catch (Exception ex) {

            // Keep real Tavily competitors even when Gemini enrichment fails.

            // Never replace missing evidence with invented content.

            log.warn(

                    "Gemini competitor enrichment failed: {}",

                    ex.getMessage()

            );

        }

    }



    private String normalizeAnalysisField(String value) {

        String text = safe(value)

                .replaceAll("\\s+", " ")

                .trim();



        if (text.isBlank()

                || text.equalsIgnoreCase("unknown")

                || text.equalsIgnoreCase("n/a")

                || text.equalsIgnoreCase("not available")

                || text.equalsIgnoreCase("not verified")

                || text.equalsIgnoreCase("unverified")) {

            return "Not enough public evidence found";

        }



        return limitText(text, 700);

    }



    private void collectTavilyResults(List<JsonNode> target, JsonNode response) {

        JsonNode results = response == null ? null : response.path("results");

        if (results != null && results.isArray()) {

            results.forEach(target::add);

        }

    }



    /**

     * Derives a small set of domain-relevant keywords from the startup's

     * own submitted data (industry, business category, idea, solution,

     * description, target market) - never a fixed keyword list for one

     * specific vertical. The founder-declared industry and business

     * category are included as whole phrases first since they are usually

     * the clearest description of what the business does; remaining

     * keywords come from frequency-ranked significant words across the

     * other free-text fields.

     */

    private List<String> extractDomainKeywords(Startup s) {

        LinkedHashSet<String> keywords = new LinkedHashSet<>();



        String industryPhrase = safe(s.getIndustry()).toLowerCase(Locale.ROOT).trim();

        if (!industryPhrase.isBlank()) {

            keywords.add(industryPhrase);

        }



        String categoryPhrase = safe(s.getBusinessCategory()).toLowerCase(Locale.ROOT).trim();

        if (!categoryPhrase.isBlank()) {

            keywords.add(categoryPhrase);

        }



        String combinedText = String.join(" ",

                safe(s.getStartupIdea()),

                safe(s.getSolution()),

                safe(s.getDescription()),

                safe(s.getTargetMarket())

        ).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ");



        Map<String, Integer> frequency = new LinkedHashMap<>();

        for (String token : combinedText.split("\\s+")) {

            if (token.length() < 4 || DOMAIN_KEYWORD_STOPWORDS.contains(token)) {

                continue;

            }

            frequency.merge(token, 1, Integer::sum);

        }



        frequency.entrySet().stream()

                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))

                .limit(8)

                .forEach(e -> keywords.add(e.getKey()));



        return new ArrayList<>(keywords);

    }



    private static final Set<String> DOMAIN_KEYWORD_STOPWORDS = Set.of(

            "the", "and", "for", "with", "that", "this", "from", "your", "our",

            "have", "will", "are", "can", "into", "using", "service", "services",

            "platform", "platforms", "app", "apps", "based", "online", "business",

            "businesses", "startup", "startups", "solution", "solutions", "company",

            "companies", "customer", "customers", "users", "user", "product",

            "products", "market", "markets", "provide", "provides", "providing",

            "offer", "offers", "offering", "help", "helps", "helping", "through",

            "across", "around", "every", "also", "more", "most", "best", "top",

            "new", "who", "what", "when", "where", "how", "their", "they", "them",

            "its", "of", "to", "in", "on", "by", "or", "as", "is", "be", "we",

            "us", "all", "any", "other", "such", "like", "via", "get", "gets",

            "getting", "make", "makes", "making", "want", "wants", "need", "needs"

    );



    private String buildCompetitorQuery(Startup s, List<String> keywords) {

        String keywordPhrase = keywordPhrase(s, keywords);

        return String.format(

                Locale.ROOT,

                "real companies %s serving %s %s",

                keywordPhrase,

                compactQueryText(s.getLocation()),

                compactQueryText(s.getTargetMarket())

        );

    }



    private String buildAlternativeCompetitorQuery(Startup s, List<String> keywords) {

        String keywordPhrase = keywordPhrase(s, keywords);

        return String.format(

                Locale.ROOT,

                "%s competitors and alternatives serving %s",

                keywordPhrase,

                compactQueryText(s.getLocation())

        );

    }



    private String keywordPhrase(Startup s, List<String> keywords) {

        if (keywords.isEmpty()) {

            return compactQueryText(s.getIndustry());

        }

        return String.join(" ", keywords.subList(0, Math.min(4, keywords.size())));

    }



    private String compactQueryText(String value) {

        String text = safe(value).replaceAll("\\s+", " ").trim();

        if (text.length() > 180) text = text.substring(0, 180);

        return text.isBlank() ? "the target market" : text;

    }



    private boolean isCredibleCompetitorResult(

            String title,

            String url,

            String content,

            Startup startup,

            List<String> keywords

    ) {

        if (title.isBlank() || !looksLikeUrl(url) || content.isBlank()) return false;



        String domain = normalizeDomain(url);

        if (domain.isBlank() || isBlockedSearchDomain(url)) return false;



        String lowerTitle = title.toLowerCase(Locale.ROOT);

        String lowerContent = content.toLowerCase(Locale.ROOT);

        String lowerUrl = url.toLowerCase(Locale.ROOT);

        String combined = lowerTitle + " " + lowerContent + " " + lowerUrl;



        if (lowerTitle.contains("top ")

                || lowerTitle.contains("best ")

                || lowerTitle.contains("list of")

                || lowerTitle.contains("alternatives to")

                || lowerTitle.contains("competitors of")

                || lowerTitle.contains("comparison")

                || lowerUrl.contains("/blog/")

                || lowerUrl.contains("/category/")

                || lowerUrl.contains("/tag/")

                || lowerUrl.contains("/search")

                || lowerUrl.contains("/compare")

                || lowerUrl.contains("/alternatives")) return false;



        String startupName = safe(startup.getName()).toLowerCase(Locale.ROOT).trim();

        if (!startupName.isBlank()

                && (lowerTitle.contains(startupName)

                || domain.contains(normalizeNameForDomain(startupName)))) return false;



        /*

         * The result must show concrete evidence that it actually relates

         * to this startup's product/industry domain, derived dynamically

         * from the startup's own industry/idea/solution/target market -

         * never a fixed keyword list for one vertical. Requiring 2 matches

         * (when enough keywords exist) keeps the same evidentiary bar the

         * old flower-specific dual-category check enforced, without being

         * tied to any one business type.

         */

        int keywordMatches = 0;

        for (String keyword : keywords) {

            if (!keyword.isBlank() && combined.contains(keyword)) {

                keywordMatches++;

            }

        }

        int requiredMatches = keywords.size() >= 2 ? 2 : 1;

        if (keywordMatches < requiredMatches) return false;



        String location = safe(startup.getLocation()).toLowerCase(Locale.ROOT);

        if (!location.isBlank()) {

            boolean geographicEvidence = false;

            for (String term : location.replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {

                if (term.length() >= 4 && combined.contains(term)) {

                    geographicEvidence = true;

                    break;

                }

            }

            if (!geographicEvidence) return false;

        }



        return true;

    }



    private String normalizeNameForDomain(String value) {

        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

    }



    private boolean isBlockedSearchDomain(String url) {

        String domain = normalizeDomain(url);

        if (domain.isBlank()) {

            return true;

        }



        return domain.equals("g2.com")

                || domain.endsWith(".g2.com")

                || domain.equals("crunchbase.com")

                || domain.endsWith(".crunchbase.com")

                || domain.equals("linkedin.com")

                || domain.endsWith(".linkedin.com")

                || domain.equals("wikipedia.org")

                || domain.endsWith(".wikipedia.org")

                || domain.equals("reddit.com")

                || domain.endsWith(".reddit.com")

                || domain.equals("medium.com")

                || domain.endsWith(".medium.com")

                || domain.equals("trustpilot.com")

                || domain.endsWith(".trustpilot.com")

                || domain.equals("capterra.com")

                || domain.endsWith(".capterra.com")

                || domain.equals("gartner.com")

                || domain.endsWith(".gartner.com")

                || domain.equals("forbes.com")

                || domain.endsWith(".forbes.com")

                || domain.equals("techcrunch.com")

                || domain.endsWith(".techcrunch.com");

    }



    private String normalizeDomain(String url) {

        try {

            URI uri = URI.create(url.trim());

            String host = uri.getHost();

            if (host == null) {

                return "";

            }



            host = host.toLowerCase(Locale.ROOT);

            if (host.startsWith("www.")) {

                host = host.substring(4);

            }

            return host;

        } catch (Exception ex) {

            return "";

        }

    }



    private String cleanCompetitorName(String title, String domain) {

        String name = title

                .replaceAll("\\s+[-|:].*$", "")

                .replaceAll("\\s+\\\\|.*$", "")

                .trim();



        if (name.length() > 100) {

            name = name.substring(0, 100).trim();

        }



        if (name.isBlank()) {

            name = domain;

        }



        return name;

    }



    private String limitText(String text, int maxLength) {

        String value = safe(text).replaceAll("\\s+", " ").trim();

        if (value.length() <= maxLength) {

            return value;

        }

        return value.substring(0, maxLength - 3).trim() + "...";

    }



    private boolean looksLikeUrl(String value) {

        String lower = safe(value).toLowerCase(Locale.ROOT);

        return lower.startsWith("https://") || lower.startsWith("http://");

    }



    private String cleanCompetitorNameFromDomain(String domain) {

        String value = safe(domain).toLowerCase(Locale.ROOT).trim();

        if (value.isBlank()) {

            return "";

        }



        String firstPart = value.split("\\\\.")[0];

        firstPart = firstPart.replaceAll("[-_]+", " ").trim();



        if (firstPart.matches("\\d+")) {

            return "";

        }



        String[] words = firstPart.split("\\s+");

        StringBuilder result = new StringBuilder();

        for (String word : words) {

            if (word.isBlank()) {

                continue;

            }

            if (!result.isEmpty()) {

                result.append(' ');

            }

            result.append(Character.toUpperCase(word.charAt(0)))

                    .append(word.substring(1));

        }

        return result.toString();

    }



    private boolean isGenericCompetitorName(String name, List<String> keywords, String domain) {

        String n = safe(name).toLowerCase(Locale.ROOT).trim();



        if (n.isBlank()

                || n.equals("competitor")

                || n.contains("established providers")

                || n.contains("local and regional businesses")

                || n.contains("digital-first alternatives")

                || n.contains("traditional/offline alternatives")

                || n.contains("competitor category")

                || n.contains("representative competitor")

                || n.contains("ai-suggested")

                || n.contains("unverified")

                || n.equals("search results")

                || n.equals("companies")

                || n.equals("products")) {

            return true;

        }



        /*

         * Reject names that are just a generic category phrase built from

         * the startup's own domain keywords (e.g. "flower delivery",

         * "order flowers online") rather than an actual brand/company

         * identity. Generated dynamically per startup rather than from a

         * fixed list for one vertical.

         */

        for (String keyword : keywords) {

            if (keyword.isBlank()) continue;

            if (n.equals(keyword)

                    || n.equals(keyword + " delivery")

                    || n.equals(keyword + " delivery online")

                    || n.equals(keyword + " delivery services")

                    || n.equals(keyword + " services")

                    || n.equals(keyword + " online")

                    || n.equals("order " + keyword + " online")

                    || n.equals("buy " + keyword + " online")

                    || n.equals("best " + keyword)) {

                return true;

            }

        }



        /*

         * Beyond the fixed/dynamic phrase matches above, also reject a name

         * that carries no identifiable company/brand identity at all - i.e.

         * every word in it is either one of the startup's own domain

         * keywords or a generic business/category connector word (e.g.

         * "service", "online", "provider"), AND the result's own domain

         * label shows no distinct brand token either. A genuine competitor

         * name or domain almost always contains at least one token that is

         * neither a keyword nor a generic connector - this check is

         * vertical-agnostic and never relies on a fixed brand list.

         */

        if (!hasIdentifiableBrandIdentity(n, keywords, domain)) {

            return true;

        }



        return false;

    }



    private static final Set<String> GENERIC_BUSINESS_CONNECTOR_WORDS = Set.of(

            "delivery", "online", "service", "services", "provider", "providers",

            "solution", "solutions", "platform", "platforms", "company", "companies",

            "store", "stores", "shop", "shops", "app", "apps", "website", "websites",

            "order", "buy", "best", "top", "near", "me", "the", "and", "for", "in",

            "of", "a", "an", "your", "our", "get", "find", "search", "marketplace",

            "business", "businesses", "vendor", "vendors", "supplier", "suppliers",

            "official", "home", "page", "co", "inc", "llc", "ltd", "pvt"

    );



    /**

     * Checks whether a candidate competitor name shows an identifiable

     * company/brand identity, rather than being purely a generic

     * description of the startup's own product/service category.

     *

     * This is deliberately generalized across any business vertical: it

     * never checks for a specific brand name. Instead it verifies that at

     * least one token - in the name itself, or failing that in the

     * result's own domain label - is neither one of the startup's derived

     * domain keywords nor a generic business/category connector word.

     */

    private boolean hasIdentifiableBrandIdentity(

            String lowerName,

            List<String> keywords,

            String domain

    ) {

        Set<String> keywordSet = new HashSet<>();

        for (String keyword : keywords) {

            if (!keyword.isBlank()) {

                keywordSet.add(keyword.toLowerCase(Locale.ROOT).trim());

            }

        }



        if (hasDistinctiveToken(lowerName, keywordSet)) {

            return true;

        }



        /*

         * Fall back to the domain label: a genuine company/brand usually

         * owns a distinct second-level domain of its own, not one built

         * purely from the startup's own keywords/connector words (e.g.

         * "flowerdeliveryonline.com"). Strip out keyword and connector

         * substrings from the label; anything left over is treated as a

         * brand signal.

         */

        String label = safe(domain).toLowerCase(Locale.ROOT).split("\\\\.")[0];

        for (String keyword : keywordSet) {

            String compact = keyword.replaceAll("\\s+", "");

            if (!compact.isBlank()) {

                label = label.replace(compact, " ");

            }

        }

        for (String connector : GENERIC_BUSINESS_CONNECTOR_WORDS) {

            label = label.replace(connector, " ");

        }

        label = label.replaceAll("[^a-z]", " ").trim();



        return !label.isBlank();

    }



    private boolean hasDistinctiveToken(String lowerText, Set<String> keywordSet) {

        for (String word : lowerText.replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {

            if (word.isBlank() || word.length() < 2) continue;

            if (keywordSet.contains(word) || GENERIC_BUSINESS_CONNECTOR_WORDS.contains(word)) continue;

            return true;

        }

        return false;

    }



    private String normalizePricing(String value) {

        String pricing = safe(value);

        if (pricing.isBlank() || pricing.equalsIgnoreCase("not available")

                || pricing.equalsIgnoreCase("unknown") || pricing.equalsIgnoreCase("n/a")) {

            return "Not publicly available";

        }

        return pricing;

    }



    // ============================================================

    // GEMINI FULL ANALYSIS

    // ============================================================



    private AiAnalysisBundle callGeminiForFullAnalysis(

            Startup s,

            List<Competitor> competitors,

            NlpExtractionResult nlp,

            MlPredictionResult prediction,

            double marketSize,

            double competitionLevel,

            double growthRate

    ) {



        String system = """

                You are VentureAI's startup business intelligence analyst.



                Return ONLY valid JSON.

                Do not use markdown.

                Do not add explanations outside the JSON.



                Use exactly this structure:



                {

                  "swot": {

                    "strengths": [string],

                    "weaknesses": [string],

                    "opportunities": [string],

                    "threats": [string]

                  },

                  "competitiveAdvantage": string,

                  "marketGap": string,

                  "differentiationStrategy": string,

                  "marketOpportunity": {

                    "marketDemand": string,

                    "growthPotential": string,

                    "customerSegments": [string],

                    "marketTrends": [string],

                    "opportunityAreas": [string],

                    "majorRisks": [string]

                  },

                  "recommendations": {

                    "business": [string],

                    "marketing": [string],

                    "pricing": [string],

                    "revenueModel": [string],

                    "product": [string],

                    "growthStrategy": [string],

                    "riskMitigation": [string],

                    "funding": [string],

                    "summary": string

                  },

                  "risks": [

                    {

                      "category": string,

                      "riskLevel": "LOW|MEDIUM|HIGH",

                      "impact": "LOW|MEDIUM|HIGH",

                      "mitigationStrategy": string

                    }

                  ],

                  "finalVerdict": string

                }



                RULES:



                1. Base every qualitative statement on the supplied startup data.



                2. Do not use generic statements that could apply to every startup.



                3. Do not invent TAM, SAM, SOM, market share, market revenue,

                   industry CAGR, competitor revenue, customer counts or

                   other numeric market facts.



                4. The supplied "market size score" is NOT actual market size.

                   Treat it only as a VentureAI model indicator.



                5. The supplied "expected growth rate" is the founder's

                   business growth assumption. Do NOT call it industry CAGR

                   or market growth unless a verified external source is

                   explicitly provided.



                6. If external market evidence is unavailable, say that

                   market data requires further verification.



                7. Do not claim that an identified competitor is verified

                   unless it appears in the supplied competitor records.



                8. Do not invent competitor revenue, market share or pricing.



                9. Customer segments must be derived from the actual target

                   market, business model and startup problem.



                10. Market trends must be relevant to the startup's actual

                    industry/business model. Do not automatically mention

                    AI, automation, subscriptions or digital adoption.



                11. Opportunities must be actionable and connected to the

                    startup's actual solution.



                12. Risks must be realistic for the startup's industry,

                    operating model and customer type.



                13. Recommendations must be practical for an early-stage

                    startup.



                14. Do not present assumptions as facts.



                15. If information is insufficient, explicitly state that

                    additional validation or external data is required.

                """;



        StringBuilder competitorText =

                new StringBuilder();



        if (competitors != null && !competitors.isEmpty()) {



            for (Competitor c : competitors) {



                competitorText

                        .append("- Name: ")

                        .append(safe(c.getName()))

                        .append("\n")

                        .append("  Description: ")

                        .append(safe(c.getDescription()))

                        .append("\n")

                        .append("  Pricing: ")

                        .append(safe(c.getPricing()))

                        .append("\n")

                        .append("  Strengths: ")

                        .append(safe(c.getStrengths()))

                        .append("\n")

                        .append("  Weaknesses: ")

                        .append(safe(c.getWeaknesses()))

                        .append("\n")

                        .append("  Market position: ")

                        .append(safe(c.getMarketPosition()))

                        .append("\n");

            }



        } else {



            competitorText.append("(No competitor records supplied)");

        }



        String prompt = """

                STARTUP INFORMATION



                Startup name:

                %s



                Industry:

                %s



                NLP-detected industry:

                %s



                Problem:

                %s



                Solution:

                %s



                Target market:

                %s



                Business model:

                %s



                Revenue model:

                %s



                Unique value proposition:

                %s



                Location:

                %s



                Extracted keywords:

                %s





                VENTUREAI MODEL INFORMATION



                BART viability score:

                %.1f/100



                BART classification:

                %s



                BART confidence:

                %.0f%%



                VentureAI market attractiveness indicator:

                %.1f/100



                VentureAI competition intensity estimate:

                %.1f/100



                Founder/business expected growth assumption:

                %.1f%%





                IMPORTANT INTERPRETATION



                The market attractiveness indicator is a VentureAI model

                estimate, not actual market revenue or TAM.



                The competition value is based on the competitor information

                available to VentureAI and may be incomplete.



                The expected growth percentage is a business projection

                supplied to the system, not automatically an industry CAGR.





                COMPETITOR RECORDS



                %s





                TASK



                Generate a startup-specific business intelligence analysis.



                Focus on:

                - actual customer problem

                - actual solution

                - target customers

                - business model

                - revenue model

                - value proposition

                - realistic competitive positioning

                - practical market opportunities

                - relevant market trends

                - operational and financial risks

                - actionable recommendations



                Never manufacture external market facts.

                """.formatted(

                safe(s.getName()),

                safe(s.getIndustry()),

                safe(nlp.industry()),

                safe(s.getProblem()),

                safe(s.getSolution()),

                safe(s.getTargetMarket()),

                safe(s.getBusinessModel()),

                safe(s.getRevenueModel()),

                safe(s.getUniqueValueProposition()),

                safe(s.getLocation()),

                nlp.keywords() == null

                        ? ""

                        : String.join(", ", nlp.keywords()),

                prediction.score(),

                prediction.classification(),

                prediction.confidence() * 100,

                marketSize,

                competitionLevel,

                growthRate,

                competitorText

        );



        JsonNode root =

                geminiClient.generateJson(

                        system,

                        prompt

                );



        // ========================================================

        // SWOT

        // ========================================================



        SwotDto swot =

                new SwotDto(

                        textList(

                                root.path("swot")

                                        .path("strengths")

                        ),

                        textList(

                                root.path("swot")

                                        .path("weaknesses")

                        ),

                        textList(

                                root.path("swot")

                                        .path("opportunities")

                        ),

                        textList(

                                root.path("swot")

                                        .path("threats")

                        )

                );



        // ========================================================

        // MARKET OPPORTUNITY

        // ========================================================



        JsonNode mo =

                root.path("marketOpportunity");



        /*

         * Do NOT calculate market-growth score as:

         *

         * growthRate * 1.5

         *

         * That would falsely convert a founder growth assumption

         * into a market opportunity score.

         *

         * The rule-based engine already provides the deterministic

         * model estimate. For the Gemini path, preserve the existing

         * market indicator supplied by VentureAI and clearly identify

         * the data as estimated.

         */



        double marketOpportunityScore =

                calculateOpportunityIndicator(

                        marketSize,

                        competitionLevel,

                        growthRate

                );



        MarketOpportunityResponse marketOpportunity =

                new MarketOpportunityResponse(

                        safe(s.getTargetMarket()),



                        mo.path("marketDemand")

                                .asText(

                                        "Demand requires further validation."

                                ),



                        mo.path("growthPotential")

                                .asText(

                                        "Growth assumption: "

                                                + Math.round(growthRate)

                                                + "%; external industry CAGR unavailable."

                                ),



                        textList(

                                mo.path("customerSegments")

                        ),



                        textList(

                                mo.path("marketTrends")

                        ),



                        textList(

                                mo.path("opportunityAreas")

                        ),



                        textList(

                                mo.path("majorRisks")

                        ),



                        marketOpportunityScore,



                        clamp(marketSize),



                        clamp(competitionLevel),



                        true

                );



        // ========================================================

        // RECOMMENDATIONS

        // ========================================================



        JsonNode rec =

                root.path("recommendations");



        RecommendationsResponse recommendations =

                new RecommendationsResponse(

                        textList(

                                rec.path("business")

                        ),

                        textList(

                                rec.path("marketing")

                        ),

                        textList(

                                rec.path("pricing")

                        ),

                        textList(

                                rec.path("revenueModel")

                        ),

                        textList(

                                rec.path("product")

                        ),

                        textList(

                                rec.path("growthStrategy")

                        ),

                        textList(

                                rec.path("riskMitigation")

                        ),

                        textList(

                                rec.path("funding")

                        ),

                        rec.path("summary")

                                .asText(

                                        "Validate the core customer, market and financial assumptions before scaling."

                                )

                );



        // ========================================================

        // RISK ANALYSIS

        // ========================================================



        List<RiskCategoryDto> risks =

                new ArrayList<>();



        root.path("risks")

                .forEach(

                        node ->

                                risks.add(

                                        new RiskCategoryDto(

                                                node.path("category")

                                                        .asText("Risk"),



                                                normalizeRiskLevel(

                                                        node.path("riskLevel")

                                                                .asText("MEDIUM")

                                                ),



                                                normalizeRiskLevel(

                                                        node.path("impact")

                                                                .asText("MEDIUM")

                                                ),



                                                node.path(

                                                                "mitigationStrategy"

                                                        )

                                                        .asText(

                                                                "Validate this risk through customer and operational evidence."

                                                        )

                                        )

                                )

                );



        /*

         * If Gemini returned no risks, create a transparent fallback.

         */

        if (risks.isEmpty()) {



            risks.add(

                    new RiskCategoryDto(

                            "Market Risk",

                            "MEDIUM",

                            "MEDIUM",

                            "Validate demand with real customers before scaling."

                    )

            );



            risks.add(

                    new RiskCategoryDto(

                            "Competition Risk",

                            competitionLevel >= 66

                                    ? "HIGH"

                                    : competitionLevel >= 33

                                    ? "MEDIUM"

                                    : "LOW",

                            competitionLevel >= 66

                                    ? "HIGH"

                                    : competitionLevel >= 33

                                    ? "MEDIUM"

                                    : "LOW",

                            "Compare the startup against verified competitors before making differentiation claims."

                    )

            );

        }



        double overall =

                risks.stream()

                        .mapToDouble(

                                r ->

                                        switch (r.riskLevel()) {

                                            case "HIGH" -> 80;

                                            case "LOW" -> 20;

                                            default -> 50;

                                        }

                        )

                        .average()

                        .orElse(50);



        RiskAnalysisResponse riskAnalysis =

                new RiskAnalysisResponse(

                        risks,

                        Math.round(overall * 10) / 10.0,

                        overall >= 66

                                ? "HIGH"

                                : overall >= 40

                                ? "MEDIUM"

                                : "LOW"

                );



        // ========================================================

        // FINAL RESPONSE

        // ========================================================



        return new AiAnalysisBundle(

                swot,



                root.path("competitiveAdvantage")

                        .asText(""),



                root.path("marketGap")

                        .asText(""),



                root.path("differentiationStrategy")

                        .asText(""),



                marketOpportunity,



                recommendations,



                riskAnalysis,



                root.path("finalVerdict")

                        .asText(

                                "Validate the startup's key assumptions with real customer and market evidence."

                        ),



                true

        );

    }



    // ============================================================

    // MARKET OPPORTUNITY INDICATOR

    // ============================================================



    private double calculateOpportunityIndicator(

            double marketSizeIndicator,

            double competitionLevel,

            double growthRate

    ) {



        /*

         * This is an internal VentureAI opportunity indicator.

         *

         * It is NOT:

         * - TAM

         * - SAM

         * - SOM

         * - market share

         * - industry CAGR

         *

         * Components:

         *

         * Market attractiveness indicator = 40%

         * Growth assumption indicator      = 25%

         * Competitive attractiveness      = 35%

         */



        double marketAttractiveness =

                clamp(marketSizeIndicator);



        double growthIndicator =

                growthIndicator(growthRate);



        double competitiveAttractiveness =

                100 - clamp(competitionLevel);



        double score =

                (marketAttractiveness * 0.40)

                        + (growthIndicator * 0.25)

                        + (competitiveAttractiveness * 0.35);



        return Math.round(

                clamp(score) * 10.0

        ) / 10.0;

    }



    // ============================================================

    // GROWTH INDICATOR

    // ============================================================



    private double growthIndicator(

            double growthRate

    ) {



        /*

         * Converts a business growth assumption into a directional

         * indicator.

         *

         * It must never be described as industry CAGR.

         */



        if (growthRate <= 0) {

            return 20;

        }



        if (growthRate <= 5) {

            return 30;

        }



        if (growthRate <= 10) {

            return 45;

        }



        if (growthRate <= 20) {

            return 60;

        }



        if (growthRate <= 30) {

            return 72;

        }



        if (growthRate <= 50) {

            return 82;

        }



        if (growthRate <= 75) {

            return 90;

        }



        return 95;

    }



    // ============================================================

    // JSON ARRAY TO STRING LIST

    // ============================================================



    private List<String> textList(

            JsonNode node

    ) {



        List<String> list =

                new ArrayList<>();



        if (

                node != null &&

                        node.isArray()

        ) {



            StreamSupport

                    .stream(

                            node.spliterator(),

                            false

                    )

                    .forEach(

                            n ->

                                    list.add(

                                            n.asText()

                                    )

                    );

        }



        return list;

    }



    // ============================================================

    // RISK LEVEL NORMALIZATION

    // ============================================================



    private String normalizeRiskLevel(

            String value

    ) {



        if (value == null) {

            return "MEDIUM";

        }



        String normalized =

                value.trim()

                        .toUpperCase();



        return switch (normalized) {

            case "HIGH" -> "HIGH";

            case "LOW" -> "LOW";

            default -> "MEDIUM";

        };

    }



    // ============================================================

    // TEXT HELPERS

    // ============================================================



    private String safe(

            String value

    ) {



        return value == null

                ? ""

                : value.trim();

    }



    private String shorten(

            String value

    ) {



        if (

                value == null ||

                        value.isBlank()

        ) {

            return "the stated problem";

        }



        String cleaned =

                value.trim();



        return cleaned.length() > 100

                ? cleaned.substring(0, 100) + "..."

                : cleaned;

    }



    private String appendUnverified(

            String value

    ) {



        String cleaned =

                safe(value);



        if (

                cleaned.toLowerCase()

                        .contains("unverified")

        ) {

            return cleaned;

        }



        return cleaned

                + " (AI-suggested, unverified)";

    }



    private String appendIfMissing(

            String value,

            String suffix

    ) {



        String cleaned =

                safe(value);



        if (

                cleaned.isBlank()

        ) {

            return suffix;

        }



        String lower =

                cleaned.toLowerCase();



        if (

                lower.contains("verify")

                        || lower.contains("unverified")

                        || lower.contains("estimated")

        ) {

            return cleaned;

        }



        return cleaned

                + " ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â "

                + suffix;

    }



    private double clamp(

            double value

    ) {



        return Math.max(

                0,

                Math.min(

                        100,

                        value

                )

        );

    }

}
