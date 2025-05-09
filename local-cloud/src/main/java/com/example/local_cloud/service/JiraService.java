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
     * Fetch Jira issues by JQL query
     * @param jql JQL query string
     * @return List of issue data as maps
     */
    List<Map<String, Object>> fetchJiraIssuesByJql(String jql);
    
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
} 