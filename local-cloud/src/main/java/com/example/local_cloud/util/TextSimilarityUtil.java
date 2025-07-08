package com.example.local_cloud.util;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for calculating text similarity metrics for duplicate detection.
 */
@Component
public class TextSimilarityUtil {
    
    private static final Pattern WORD_SPLITTER = Pattern.compile("\\W+");
    
    /**
     * Calculate Jaccard similarity coefficient between two text strings.
     * Jaccard similarity = (intersection size) / (union size)
     * 
     * @param text1 First text string
     * @param text2 Second text string
     * @return Similarity score between 0 (no similarity) and 1 (identical)
     */
    public double calculateJaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // Convert texts to sets of words
        Set<String> words1 = tokenizeText(text1);
        Set<String> words2 = tokenizeText(text2);
        
        if (words1.isEmpty() && words2.isEmpty()) {
            return 1.0; // Both empty = identical
        }
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0; // One empty, one not = no similarity
        }
        
        // Calculate intersection size
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        // Calculate union size
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        // Jaccard coefficient = size of intersection / size of union
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Calculate cosine similarity between two text strings using TF vectors.
     * 
     * @param text1 First text string
     * @param text2 Second text string
     * @return Similarity score between 0 (no similarity) and 1 (identical)
     */
    public double calculateCosineSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        // Tokenize the texts
        List<String> tokens1 = tokenizeTextToList(text1);
        List<String> tokens2 = tokenizeTextToList(text2);
        
        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0; // Both empty = identical
        }
        
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0; // One empty, one not = no similarity
        }
        
        // Get all unique terms
        Set<String> uniqueTerms = new HashSet<>(tokens1);
        uniqueTerms.addAll(tokens2);
        
        // Calculate term frequencies for both texts
        Map<String, Integer> termFreq1 = calculateTermFrequencies(tokens1);
        Map<String, Integer> termFreq2 = calculateTermFrequencies(tokens2);
        
        // Calculate dot product and magnitudes
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        for (String term : uniqueTerms) {
            int freq1 = termFreq1.getOrDefault(term, 0);
            int freq2 = termFreq2.getOrDefault(term, 0);
            
            dotProduct += freq1 * freq2;
            magnitude1 += freq1 * freq1;
            magnitude2 += freq2 * freq2;
        }
        
        // Calculate cosine similarity
        if (magnitude1 > 0 && magnitude2 > 0) {
            return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
        } else {
            return 0.0;
        }
    }
    
    /**
     * Calculate a combined similarity score using multiple metrics.
     * This gives a more robust similarity measurement than any single metric.
     * 
     * @param text1 First text string
     * @param text2 Second text string
     * @return Combined similarity score between 0 (no similarity) and 1 (identical)
     */
    public double calculateSimilarity(String text1, String text2) {
        // Calculate different similarity metrics
        double jaccardSimilarity = calculateJaccardSimilarity(text1, text2);
        double cosineSimilarity = calculateCosineSimilarity(text1, text2);
        
        // Return weighted average (can adjust weights as needed)
        return 0.4 * jaccardSimilarity + 0.6 * cosineSimilarity;
    }
    
    /**
     * Calculate a weighted similarity score between two issues based on their
     * summary and description texts. Handles cases where either field may be empty.
     * 
     * @param summary1 Summary of first issue (may be null/empty)
     * @param description1 Description of first issue (may be null/empty)
     * @param summary2 Summary of second issue (may be null/empty)
     * @param description2 Description of second issue (may be null/empty)
     * @return Weighted similarity score between 0 (no similarity) and 1 (identical)
     */
    public double calculateIssueSimilarity(String summary1, String description1, 
                                          String summary2, String description2) {
        // Check if fields are null or empty
        boolean hasSummary1 = summary1 != null && !summary1.trim().isEmpty();
        boolean hasDescription1 = description1 != null && !description1.trim().isEmpty();
        boolean hasSummary2 = summary2 != null && !summary2.trim().isEmpty();
        boolean hasDescription2 = description2 != null && !description2.trim().isEmpty();
        
        // If comparing only summaries
        if (hasSummary1 && hasSummary2 && (!hasDescription1 || !hasDescription2)) {
            return calculateSimilarity(summary1, summary2);
        }
        
        // If comparing only descriptions
        if (hasDescription1 && hasDescription2 && (!hasSummary1 || !hasSummary2)) {
            return calculateSimilarity(description1, description2);
        }
        
        // If comparing both fields
        if (hasSummary1 && hasSummary2 && hasDescription1 && hasDescription2) {
            double summarySimilarity = calculateSimilarity(summary1, summary2);
            double descriptionSimilarity = calculateSimilarity(description1, description2);
            
            // Weighted average (summary is more important than description)
            return 0.6 * summarySimilarity + 0.4 * descriptionSimilarity;
        }
        
        // If comparing mixed fields (e.g., summary to description)
        String text1 = hasSummary1 ? summary1 : (hasDescription1 ? description1 : "");
        String text2 = hasSummary2 ? summary2 : (hasDescription2 ? description2 : "");
        
        return calculateSimilarity(text1, text2);
    }
    
    /**
     * Split text into lowercase words, filtering out short tokens
     */
    private Set<String> tokenizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        String[] words = WORD_SPLITTER.split(text.toLowerCase());
        return Arrays.stream(words)
                .filter(word -> word.length() > 2) // Filter out very short tokens
                .collect(Collectors.toSet());
    }
    
    /**
     * Split text into lowercase words as a list (preserving duplicates)
     */
    private List<String> tokenizeTextToList(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String[] words = WORD_SPLITTER.split(text.toLowerCase());
        return Arrays.stream(words)
                .filter(word -> word.length() > 2) // Filter out very short tokens
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate term frequencies for a list of tokens
     */
    private Map<String, Integer> calculateTermFrequencies(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            frequencies.put(token, frequencies.getOrDefault(token, 0) + 1);
        }
        return frequencies;
    }
}
