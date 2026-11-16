package com.resumeanalyzer.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Plain JUnit 5 test — no Spring context needed, since ScoringEngine has zero
// framework dependencies. This is intentional: fast, isolated unit tests.
class ScoringEngineTest {

    @Test
    void identicalTextsScoreMaximum() {
        String text = "Java Spring Boot developer with REST API experience";
        var result = ScoringEngine.computeMatch(text, text);
        assertEquals(100.0, result.score());
        assertTrue(result.missingKeywords().isEmpty());
    }

    @Test
    void emptyResumeScoresZero() {
        var result = ScoringEngine.computeMatch("", "Java Spring Boot developer needed");
        assertEquals(0.0, result.score());
        assertFalse(result.missingKeywords().isEmpty());
    }

    @Test
    void relevantResumeScoresHigherThanIrrelevantOne() {
        String jd = "Backend developer with Spring Boot, Java, REST APIs, MySQL, Docker, AWS";
        String relevantResume = "Experienced in Spring Boot, Java, REST API design, and MySQL";
        String irrelevantResume = "Graphic designer skilled in Photoshop, Illustrator, branding";

        double relevantScore = ScoringEngine.computeMatch(relevantResume, jd).score();
        double irrelevantScore = ScoringEngine.computeMatch(irrelevantResume, jd).score();

        assertTrue(relevantScore > irrelevantScore);
    }

    @Test
    void missingKeywordsOnlyContainsWordsAbsentFromResume() {
        String jd = "Requires Docker and Kubernetes experience";
        String resume = "Experienced with Docker and CI/CD pipelines";

        var result = ScoringEngine.computeMatch(resume, jd);

        assertFalse(result.missingKeywords().contains("docker")); // present in resume -> not missing
        assertTrue(result.missingKeywords().contains("kubernetes")); // absent from resume -> missing
    }

    @Test
    void missingKeywordsLeaveOutJobDescriptionFiller() {
        String jd = "We are looking for strong knowledge of Docker and Kubernetes. "
                + "Familiarity with AWS is preferred and must include testing. Candidates required.";

        var result = ScoringEngine.computeMatch("Java developer", jd);

        assertTrue(result.missingKeywords().containsAll(java.util.List.of("docker", "kubernetes", "aws", "testing")));
        for (String filler : java.util.List.of("looking", "strong", "knowledge", "familiarity",
                "preferred", "include", "candidates", "required")) {
            assertFalse(result.missingKeywords().contains(filler), filler + " should not be listed");
        }
    }

    @Test
    void fillerWordsCannotCrowdRealKeywordsOutOfTheCap() {
        // 17 filler words, each repeated 3 times, outrank "aws" and "docker" (said once) by frequency.
        String filler = "required strong knowledge familiarity preferred looking include candidate excellent "
                + "proven ability responsible understanding working desired ideal efficient ";
        String jd = filler.repeat(3) + "aws docker";

        var result = ScoringEngine.computeMatch("Java developer", jd);

        assertEquals(2, result.missingKeywords().stream()
                .filter(k -> k.equals("aws") || k.equals("docker")).count());
        assertTrue(result.missingKeywords().stream().noneMatch(JobDescriptionFiller::isFiller));
    }

    @Test
    void fillerWordsDoNotAffectTheScore() {
        double plain = ScoringEngine.computeMatch("Java Spring Docker", "Java Spring Kubernetes").score();

        double withFiller = ScoringEngine.computeMatch(
                "Java Spring Docker strong knowledge required",
                "Java Spring Kubernetes looking preferred experience").score();

        assertEquals(plain, withFiller);
    }

    @Test
    void textMadeOnlyOfFillerScoresZero() {
        var result = ScoringEngine.computeMatch("strong knowledge required", "strong knowledge required");

        assertEquals(0.0, result.score());
        assertTrue(result.missingKeywords().isEmpty());
    }

    @Test
    void textMadeOfSymbolTermsStillScores() {
        // "C++" used to be stripped to nothing, so this scored 0.
        assertEquals(100.0, ScoringEngine.computeMatch("C++ developer", "C++ developer").score());
    }

    @Test
    void missingKeywordsShowWholeTermsInsteadOfFragments() {
        var missing = ScoringEngine.computeMatch("Java developer",
                "CI/CD, problem solving and Node.js").missingKeywords();

        assertTrue(missing.containsAll(java.util.List.of("ci/cd", "problem solving", "node.js")));
        for (String fragment : java.util.List.of("ci", "cd", "problem", "solving", "node", "js")) {
            assertFalse(missing.contains(fragment), fragment + " is a fragment of a whole term");
        }
    }

    @Test
    void differentSpellingsOfATermMatchEachOther() {
        var result = ScoringEngine.computeMatch("Experienced with NodeJS and back-end development",
                "Node.js backend");

        assertTrue(result.missingKeywords().isEmpty());
    }

        // ---- Score = share of the job description's keywords found in the resume ----

    @Test
    void scoreIsTheShareOfJobKeywordsFoundInTheResume() {
        var result = ScoringEngine.computeMatch("Java Spring", "Java Spring Docker Kubernetes");

        assertEquals(50.0, result.score());
    }

    @Test
    void coveringEveryRequirementScoresMaximumEvenWithLotsOfOtherContent() {
        String resume = "Built dashboards, wrote reports, organised events, led a club of forty students, "
                + "mentored juniors and presented demos. Skills: Java, Spring, Docker, MySQL, React, Git.";

        var result = ScoringEngine.computeMatch(resume, "Java Spring Docker");

        assertEquals(100.0, result.score());
        assertTrue(result.missingKeywords().isEmpty());
    }

    @Test
    void extraResumeContentNeverLowersTheScore() {
        String jd = "Java Spring Docker Kubernetes";

        double plain = ScoringEngine.computeMatch("Java Spring", jd).score();
        double padded = ScoringEngine.computeMatch("Java Spring plus a long paragraph about hobbies, "
                + "travel, photography and cooking that has nothing to do with the role", jd).score();

        assertEquals(plain, padded);
    }

    @Test
    void anUnrelatedResumeScoresZero() {
        var result = ScoringEngine.computeMatch("Graphic designer skilled in Photoshop and Illustrator",
                "Java Spring Docker");

        assertEquals(0.0, result.score());
    }

    @Test
    void aJobKeywordRepeatedManyTimesStillCountsOnce() {
        var result = ScoringEngine.computeMatch("Java", "Java Java Java Docker");

        assertEquals(50.0, result.score());
    }

    @Test
    void scoreIsRoundedToOneDecimal() {
        assertEquals(33.3, ScoringEngine.computeMatch("Java", "Java Spring Docker").score());
        assertEquals(66.7, ScoringEngine.computeMatch("Java Spring", "Java Spring Docker").score());
    }

    @Test
    void caseAndPunctuationDoNotAffectMatching() {
        assertEquals(100.0, ScoringEngine.computeMatch("JAVA, spring!", "java Spring").score());
    }

    @Test
    void anEmptyJobDescriptionScoresZeroWithNothingMissing() {
        var result = ScoringEngine.computeMatch("Java Spring Docker", "");

        assertEquals(0.0, result.score());
        assertTrue(result.missingKeywords().isEmpty());
    }

    @Test
    void missingKeywordsAreRankedByHowOftenTheJobMentionsThem() {
        var result = ScoringEngine.computeMatch("Java", "kafka redis redis docker docker docker");

        assertEquals(java.util.List.of("docker", "redis", "kafka"), result.missingKeywords());
    }

    @Test
    void keywordsMentionedEquallyOftenKeepTheJobDescriptionOrder() {
        var result = ScoringEngine.computeMatch("Java", "zeta alpha mike");

        assertEquals(java.util.List.of("zeta", "alpha", "mike"), result.missingKeywords());
    }

    @Test
    void missingKeywordsAreCappedAtFifteen() {
        String jd = "alpha bravo charlie delta echo foxtrot golf hotel india juliet "
                + "kilo lima mike november oscar papa quebec romeo sierra tango";

        var result = ScoringEngine.computeMatch("Java", jd);

        assertEquals(15, result.missingKeywords().size());
        assertEquals("alpha", result.missingKeywords().get(0));
        assertEquals("oscar", result.missingKeywords().get(14));
    }

    @Test
    void scoreAndMissingKeywordsTellTheSameStory() {
        String jd = "Java Spring Docker";

        var complete = ScoringEngine.computeMatch("Java Spring Docker", jd);
        var partial = ScoringEngine.computeMatch("Java Spring", jd);

        assertEquals(100.0, complete.score());
        assertTrue(complete.missingKeywords().isEmpty());
        assertTrue(partial.score() < 100.0);
        assertEquals(java.util.List.of("docker"), partial.missingKeywords());
    }
}
