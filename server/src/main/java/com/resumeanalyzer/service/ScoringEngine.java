package com.resumeanalyzer.service;

import java.util.*;

/**
 * Compares a resume against a job description by keyword coverage: the match score is the share
 * of the job description's distinct keywords that also appear in the resume.
 *
 * Coverage answers the question the user is asking, "does my resume cover what this job asks
 * for?". A similarity measure such as cosine similarity also penalises resume content that has
 * nothing to do with the job, so a resume containing every requirement still scored far below 100.
 *
 * Missing keywords are ranked by how often the job description mentions them. Words that fill job
 * postings but are not skills are removed first (see JobDescriptionFiller).
 */
public final class ScoringEngine {

    private static final int MAX_MISSING_KEYWORDS = 15;

    private ScoringEngine() {}

    public record MatchResult(double score, List<String> missingKeywords) {}

    public static MatchResult computeMatch(String resumeText, String jdText) {
        Set<String> resumeTerms = new HashSet<>(JobDescriptionFiller.withoutFiller(TextTokenizer.tokenize(resumeText)));
        Map<String, Integer> jdCounts = countTerms(JobDescriptionFiller.withoutFiller(TextTokenizer.tokenize(jdText)));

        if (jdCounts.isEmpty()) {
            return new MatchResult(0.0, List.of());
        }

        long found = jdCounts.keySet().stream().filter(resumeTerms::contains).count();
        double score = Math.round(found * 1000.0 / jdCounts.size()) / 10.0; // one decimal place, out of 100

        return new MatchResult(score, findMissingKeywords(jdCounts, resumeTerms));
    }

    /** Counts each term, keeping the order in which the terms first appear in the text. */
    private static Map<String, Integer> countTerms(List<String> tokens) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }

    /** Job description terms the resume doesn't contain, most mentioned first; ties keep the job description's order. */
    private static List<String> findMissingKeywords(Map<String, Integer> jdCounts, Set<String> resumeTerms) {
        return jdCounts.entrySet().stream()
                .filter(entry -> !resumeTerms.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_MISSING_KEYWORDS)
                .map(Map.Entry::getKey)
                .toList();
    }
}