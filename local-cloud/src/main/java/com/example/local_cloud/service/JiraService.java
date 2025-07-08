package com.example.local_cloud.service;

import java.util.List;
import java.util.Map;

public interface JiraService {
    
    /**
     * Fetch Jira issues by their keys
     * @param issueKeys Array of Jira issue keys (e.g., "PROJ-123")
     * @return List of issue data as maps
     */
    List<Map<String, Object>> fetchJiraIssues(String[] issueKeys);
    
    /**
     * Fetch Jira issues by JQL query with default maximum results
     * @param jql JQL query string
     * @return List of issue data as maps
     */
    List<Map<String, Object>> fetchJiraIssuesByJql(String jql);
    
    /**
     * Fetch Jira issues by JQL query with custom maximum results
     * @param jql JQL query string
     * @param maxResults Maximum number of results to return
     * @return List of issue data as maps
     */
    List<Map<String, Object>> fetchJiraIssuesByJql(String jql, int maxResults);
    
    /**
     * Get all available JIRA filters from the JSON configuration
     * @return Map of filter names to JQL queries
     */
    Map<String, String> getJiraFilters();
    
    /**
     * Get a specific JIRA filter by name
     * @param filterName The name of the filter to get
     * @return The JQL query string or null if not found
     */
    String getJiraFilterByName(String filterName);
    
    /**
     * Fetch detailed Jira issues by JQL query with description field for duplicate detection
     * @param jql JQL query string
     * @param maxResults Maximum number of results to return
     * @return List of detailed issue data as maps including descriptions
     */
    List<Map<String, Object>> fetchDetailedJiraIssuesByJql(String jql, int maxResults);
    
    /**
     * Check for duplicate issues based on summary and description similarity within specified project keys
     * @param summary The summary text to check
     * @param description The description text to check
     * @param projectKeys Array of project keys to search within (e.g. "DUKE", "RND")
     * @param minSimilarity The minimum similarity score (0.0-1.0) required to consider an issue as duplicate
     * @param maxResults Maximum number of potential duplicates to return
     * @return List of potential duplicate issues ranked by similarity
     */
    List<Map<String, Object>> findPotentialDuplicateIssues(String summary, String description, 
                                                          String[] projectKeys, double minSimilarity, int maxResults);
                                                          
    /**
     * Check for duplicate issues based on summary and description similarity using a custom JQL query
     * @param summary The summary text to check
     * @param description The description text to check
     * @param jqlQuery Custom JQL query to filter issues to check against
     * @param minSimilarity The minimum similarity score (0.0-1.0) required to consider an issue as duplicate
     * @param maxResults Maximum number of potential duplicates to return
     * @return List of potential duplicate issues ranked by similarity
     */
    List<Map<String, Object>> findPotentialDuplicateIssuesWithJql(String summary, String description, 
                                                                String jqlQuery, double minSimilarity, int maxResults);
    
    /**
     * Check for duplicate issues using a single text input that will be compared against both summary and description
     * @param text The text to compare against both summary and description fields
     * @param projectKeys Array of project keys to search within (e.g. "DUKE", "RND")
     * @param minSimilarity The minimum similarity score (0.0-1.0) required to consider an issue as duplicate
     * @param maxResults Maximum number of potential duplicates to return
     * @return List of potential duplicate issues ranked by similarity
     */
    List<Map<String, Object>> findPotentialDuplicatesWithSingleText(String text, String[] projectKeys, 
                                                                   double minSimilarity, int maxResults);
    
    /**
     * Check for duplicate issues using a single text input and a custom JQL query
     * @param text The text to compare against both summary and description fields
     * @param jqlQuery Custom JQL query to filter issues to check against
     * @param minSimilarity The minimum similarity score (0.0-1.0) required to consider an issue as duplicate
     * @param maxResults Maximum number of potential duplicates to return
     * @return List of potential duplicate issues ranked by similarity
     */
    List<Map<String, Object>> findPotentialDuplicatesWithSingleTextAndJql(String text, String jqlQuery,
                                                                         double minSimilarity, int maxResults);
} 