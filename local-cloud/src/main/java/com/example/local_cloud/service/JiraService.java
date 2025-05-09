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
} 