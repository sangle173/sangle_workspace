package com.example.local_cloud.service;

import com.example.local_cloud.config.EnvConfig;
import com.example.local_cloud.util.TextSimilarityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JiraServiceImpl implements JiraService {

    private static final Logger logger = LoggerFactory.getLogger(JiraServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final EnvConfig envConfig;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TextSimilarityUtil similarityUtil;
    
    @Value("${jira.url:https://jira.sonos.com}")
    private String jiraUrl;
    
    // Constants for environment variable keys
    private static final String JIRA_TOKEN_ENV_KEY = "JIRA_API_TOKEN";
    // Path to the JIRA filter JSON file
    private static final String JIRA_FILTER_JSON_PATH = System.getProperty("user.home") + "/Desktop/Env/jira_filter.json";
    
    @Autowired
    public JiraServiceImpl(EnvConfig envConfig, ResourceLoader resourceLoader, TextSimilarityUtil similarityUtil) {
        this.envConfig = envConfig;
        this.resourceLoader = resourceLoader;
        this.similarityUtil = similarityUtil;
    }

    @Override
    public List<Map<String, Object>> fetchJiraIssues(String[] issueKeys) {
        List<Map<String, Object>> issuesData = new ArrayList<>();
        
        // Fetch token from .env file
        String jiraToken = envConfig.getEnvValue(JIRA_TOKEN_ENV_KEY, "");
        if (jiraToken.isEmpty()) {
            logger.warn("JIRA API token not found in .env file. Please add '{}=your_token' to {}/Desktop/Env/.env",
                    JIRA_TOKEN_ENV_KEY, System.getProperty("user.home"));
        }
        
        for (String issueKey : issueKeys) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + jiraToken);
                headers.set("Accept", "application/json");
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                String url = jiraUrl + "/rest/api/latest/issue/" + issueKey;
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class
                );
                
                Map<String, Object> issue = response.getBody();
                
                if (issue != null) {
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    Map<String, Object> issueData = new HashMap<>();
                    
                    issueData.put("key", issue.get("key"));
                    issueData.put("summary", getNestedValue(fields, "summary", "No summary"));
                    
                    // Status
                    Map<String, Object> status = (Map<String, Object>) fields.get("status");
                    issueData.put("status", status != null ? status.get("name") : "Unknown");
                    
                    // Priority - removed from display but keeping data for future use
                    Map<String, Object> priority = (Map<String, Object>) fields.get("priority");
                    issueData.put("priority", priority != null ? priority.get("name") : "None");
                    
                    // Issue Type
                    Map<String, Object> issueType = (Map<String, Object>) fields.get("issuetype");
                    issueData.put("issue_type", issueType != null ? issueType.get("name") : "Unknown");
                    
                    // Reporter
                    Map<String, Object> reporter = (Map<String, Object>) fields.get("reporter");
                    issueData.put("reporter", reporter != null ? reporter.get("displayName") : "Unknown");
                    
                    // Created Date
                    String created = (String) fields.get("created");
                    if (created != null) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = inputFormat.parse(created);
                            issueData.put("created", outputFormat.format(date));
                        } catch (Exception e) {
                            issueData.put("created", created);
                        }
                    } else {
                        issueData.put("created", "Unknown");
                    }
                    
                    // Assignee
                    Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                    issueData.put("assignee", assignee != null ? assignee.get("displayName") : "Unassigned");
                    
                    // QA Contact - use the correct field ID
                    Map<String, Object> qaContact = (Map<String, Object>) fields.get("customfield_10408");
                    issueData.put("qa_contact", qaContact != null ? qaContact.get("displayName") : "Not Set");
                    
                    // Labels
                    List<String> labels = (List<String>) fields.get("labels");
                    issueData.put("labels", labels != null && !labels.isEmpty() ? String.join(", ", labels) : "None");
                    
                    // Teams (customfield_12600)
                    List<Map<String, Object>> teamsField = (List<Map<String, Object>>) fields.get("customfield_12600");
                    if (teamsField != null && !teamsField.isEmpty()) {
                        List<String> teamNames = new ArrayList<>();
                        for (Map<String, Object> team : teamsField) {
                            Object value = team.get("value");
                            if (value != null) teamNames.add(value.toString());
                        }
                        issueData.put("teams", String.join(", ", teamNames));
                    } else {
                        issueData.put("teams", "Not Assigned");
                    }
                    
                    issuesData.add(issueData);
                }
                
            } catch (RestClientException e) {
                logger.error("Error fetching issue {}: {}", issueKey, e.getMessage());
                
                // Add error placeholder
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("key", issueKey);
                errorData.put("summary", "Error fetching issue");
                errorData.put("status", "N/A");
                errorData.put("priority", "N/A");
                errorData.put("issue_type", "N/A");
                errorData.put("reporter", "N/A");
                errorData.put("created", "N/A");
                errorData.put("assignee", "N/A");
                errorData.put("qa_contact", "N/A");
                errorData.put("labels", "N/A");
                errorData.put("teams", "N/A");
                
                issuesData.add(errorData);
            }
        }
        
        return issuesData;
    }
    
    @Override
    public List<Map<String, Object>> fetchJiraIssuesByJql(String jql) {
        return fetchJiraIssuesByJql(jql, 200); // Default to 200 results max
    }
    
    @Override
    public List<Map<String, Object>> fetchJiraIssuesByJql(String jql, int maxResults) {
        List<Map<String, Object>> issuesData = new ArrayList<>();
        
        // Fetch token from .env file
        String jiraToken = envConfig.getEnvValue(JIRA_TOKEN_ENV_KEY, "");
        if (jiraToken.isEmpty()) {
            logger.warn("JIRA API token not found in .env file. Please add '{}=your_token' to {}/Desktop/Env/.env",
                    JIRA_TOKEN_ENV_KEY, System.getProperty("user.home"));
            return issuesData; // Return empty list if no token
        }
        logger.info("JIRA API token is available and will be used for authentication");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jiraToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Initialize pagination variables
            int startAt = 0;
            int pageSize = 50; // Jira API recommended page size
            int totalIssues = Integer.MAX_VALUE; // Will be updated with actual count
            
            // Loop until we've retrieved all issues or reached maxResults
            while (startAt < totalIssues && issuesData.size() < maxResults) {
                // Build URL with JQL parameter and pagination
                String url = UriComponentsBuilder
                    .fromUriString(jiraUrl + "/rest/api/latest/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", Math.min(pageSize, maxResults - issuesData.size()))
                    .queryParam("startAt", startAt)
                    .build()
                    .toUriString();
                
                logger.info("Jira API endpoint: {}", jiraUrl);
                logger.info("Fetching JIRA issues with complete URL: {}", url);
                ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class
                );
                
                Map<String, Object> searchResult = response.getBody();
                
                if (searchResult == null || !searchResult.containsKey("issues")) {
                    break; // No more results or error
                }
                
                // Update pagination variables
                totalIssues = ((Number)searchResult.getOrDefault("total", 0)).intValue();
                List<Map<String, Object>> issues = (List<Map<String, Object>>) searchResult.get("issues");
                
                if (issues.isEmpty()) {
                    break; // No more issues
                }
                
                // Process issues for this page
                for (Map<String, Object> issue : issues) {
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    Map<String, Object> issueData = new HashMap<>();
                    
                    issueData.put("key", issue.get("key"));
                    issueData.put("summary", getNestedValue(fields, "summary", "No summary"));
                    
                    // Status
                    Map<String, Object> status = (Map<String, Object>) fields.get("status");
                    issueData.put("status", status != null ? status.get("name") : "Unknown");
                    
                    // Priority - removed from display but keeping data for future use
                    Map<String, Object> priority = (Map<String, Object>) fields.get("priority");
                    issueData.put("priority", priority != null ? priority.get("name") : "None");
                    
                    // Issue Type
                    Map<String, Object> issueType = (Map<String, Object>) fields.get("issuetype");
                    issueData.put("issue_type", issueType != null ? issueType.get("name") : "Unknown");
                    
                    // Reporter
                    Map<String, Object> reporter = (Map<String, Object>) fields.get("reporter");
                    issueData.put("reporter", reporter != null ? reporter.get("displayName") : "Unknown");
                    
                    // Created Date
                    String created = (String) fields.get("created");
                    if (created != null) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = inputFormat.parse(created);
                            issueData.put("created", outputFormat.format(date));
                        } catch (Exception e) {
                            issueData.put("created", created);
                        }
                    } else {
                        issueData.put("created", "Unknown");
                    }
                    
                    // Assignee
                    Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                    issueData.put("assignee", assignee != null ? assignee.get("displayName") : "Unassigned");
                    
                    // QA Contact - use the correct field ID
                    Map<String, Object> qaContact = (Map<String, Object>) fields.get("customfield_10408");
                    issueData.put("qa_contact", qaContact != null ? qaContact.get("displayName") : "Not Set");
                    
                    // Teams (customfield_12600)
                    List<Map<String, Object>> teamsField = (List<Map<String, Object>>) fields.get("customfield_12600");
                    if (teamsField != null && !teamsField.isEmpty()) {
                        List<String> teamNames = new ArrayList<>();
                        for (Map<String, Object> team : teamsField) {
                            Object value = team.get("value");
                            if (value != null) teamNames.add(value.toString());
                        }
                        issueData.put("teams", String.join(", ", teamNames));
                    } else {
                        issueData.put("teams", "Not Assigned");
                    }
                    
                    issuesData.add(issueData);
                }
                
                // Move to next page
                startAt += issues.size();
                logger.debug("Fetched {} issues so far of {} total", issuesData.size(), totalIssues);
            }
            
            // Log the total number of issues retrieved
            logger.info("Retrieved {} JIRA issues for JQL: {}", issuesData.size(), jql);
            
        } catch (RestClientException e) {
            logger.error("Error fetching issues with JQL {}: {}", jql, e.getMessage());
        }
        
        return issuesData;
    }
    
    @Override
    public Map<String, String> getJiraFilters() {
        try {
            File filterFile = new File(JIRA_FILTER_JSON_PATH);
            if (!filterFile.exists()) {
                logger.warn("JIRA filter file not found at {}", JIRA_FILTER_JSON_PATH);
                return new HashMap<>();
            }
            return objectMapper.readValue(filterFile, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            logger.error("Error reading JIRA filter file: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public String getJiraFilterByName(String filterName) {
        Map<String, String> filters = getJiraFilters();
        return filters.getOrDefault(filterName, null);
    }
    
    @Override
    public List<Map<String, Object>> fetchDetailedJiraIssuesByJql(String jql, int maxResults) {
        List<Map<String, Object>> issuesData = new ArrayList<>();
        
        // Fetch token from .env file
        String jiraToken = envConfig.getEnvValue(JIRA_TOKEN_ENV_KEY, "");
        if (jiraToken.isEmpty()) {
            logger.warn("JIRA API token not found in .env file. Please add '{}=your_token' to {}/Desktop/Env/.env",
                    JIRA_TOKEN_ENV_KEY, System.getProperty("user.home"));
            return issuesData; // Return empty list if no token
        }
        logger.info("JIRA API token is available and will be used for authentication");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jiraToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Initialize pagination variables
            int startAt = 0;
            int pageSize = 50; // Jira API recommended page size
            int totalIssues = Integer.MAX_VALUE; // Will be updated with actual count
            
            // Loop until we've retrieved all issues or reached maxResults
            while (startAt < totalIssues && issuesData.size() < maxResults) {
                // Build URL with JQL parameter and pagination
                // Include fields we need for duplicate detection
                String url = UriComponentsBuilder
                    .fromUriString(jiraUrl + "/rest/api/latest/search")
                    .queryParam("jql", jql)
                    .queryParam("maxResults", Math.min(pageSize, maxResults - issuesData.size()))
                    .queryParam("startAt", startAt)
                    .queryParam("fields", "summary,description,key,status,issuetype,created,updated,reporter,project")
                    .build()
                    .toUriString();
                
                logger.info("Jira API endpoint: {}", jiraUrl);
                logger.info("Fetching detailed JIRA issues with complete URL: {}", url);
                
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                Map<String, Object> searchResult = response.getBody();
                
                if (searchResult == null || !searchResult.containsKey("issues")) {
                    break; // No more results or error
                }
                
                // Update pagination variables
                totalIssues = ((Number)searchResult.getOrDefault("total", 0)).intValue();
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issues = (List<Map<String, Object>>) searchResult.get("issues");
                
                if (issues.isEmpty()) {
                    break; // No more issues
                }
                
                // Process issues for this page
                for (Map<String, Object> issue : issues) {
                    Map<String, Object> issueData = new HashMap<>();
                    
                    issueData.put("key", issue.get("key"));
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
                    
                    // Extract fields needed for duplicate detection
                    issueData.put("summary", getNestedValue(fields, "summary", "No summary"));
                    issueData.put("description", getNestedValue(fields, "description", ""));
                    
                    // Status
                    @SuppressWarnings("unchecked")
                    Map<String, Object> status = (Map<String, Object>) fields.get("status");
                    issueData.put("status", status != null ? status.get("name") : "Unknown");
                    
                    // Issue Type
                    @SuppressWarnings("unchecked")
                    Map<String, Object> issueType = (Map<String, Object>) fields.get("issuetype");
                    issueData.put("issue_type", issueType != null ? issueType.get("name") : "Unknown");
                    
                    // Reporter
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reporter = (Map<String, Object>) fields.get("reporter");
                    issueData.put("reporter", reporter != null ? reporter.get("displayName") : "Unknown");
                    
                    // Created Date
                    String created = (String) fields.get("created");
                    if (created != null) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = inputFormat.parse(created);
                            issueData.put("created", outputFormat.format(date));
                        } catch (Exception e) {
                            issueData.put("created", created);
                        }
                    } else {
                        issueData.put("created", "Unknown");
                    }
                    
                    // Project
                    @SuppressWarnings("unchecked")
                    Map<String, Object> project = (Map<String, Object>) fields.get("project");
                    issueData.put("project", project != null ? project.get("key") : "Unknown");
                    
                    issuesData.add(issueData);
                }
                
                // Move to next page
                startAt += issues.size();
                logger.debug("Fetched {} issues so far of {} total", issuesData.size(), totalIssues);
            }
            
            // Log the total number of issues retrieved
            logger.info("Retrieved {} detailed JIRA issues for JQL: {}", issuesData.size(), jql);
            
        } catch (RestClientException e) {
            logger.error("Error fetching detailed issues with JQL {}: {}", jql, e.getMessage());
        }
        
        return issuesData;
    }
    
    @Override
    public List<Map<String, Object>> findPotentialDuplicateIssues(String summary, String description, 
                                                           String[] projectKeys, double minSimilarity, int maxResults) {
        // Check if we have at least one non-empty field to search with
        if ((summary == null || summary.trim().isEmpty()) && 
            (description == null || description.trim().isEmpty())) {
            logger.warn("Both summary and description are empty - cannot perform similarity search");
            return Collections.emptyList();
        }
        
        // Build JQL to search for issues in the specified projects
        String projectJql = projectKeys != null && projectKeys.length > 0 ?
            "project in (" + String.join(", ", projectKeys) + ")" : "";
        
        // Build a JQL that will find potentially similar issues
        String searchJql = projectJql.isEmpty() ? "" : projectJql + " AND ";
        
        // Use summary if available, otherwise try to extract keywords from description
        String searchText = summary != null && !summary.trim().isEmpty() ? 
            summary : description.trim();
        
        // Extract keywords for searching
        String keywords = extractKeywords(searchText, 5);
        searchJql += "(summary ~ \"" + keywords.replace("\"", "\\\"") + "\"";
        
        // If searching on description, also include description search
        if (description != null && !description.trim().isEmpty() && 
            keywords.length() > 10) { // Only add if we have substantial keywords
            searchJql += " OR description ~ \"" + keywords.replace("\"", "\\\"") + "\"";
        }
        searchJql += ")";
        
        logger.info("Using search JQL: {}", searchJql);
        
        // Fetch candidate issues
        List<Map<String, Object>> candidates = fetchDetailedJiraIssuesByJql(searchJql, 100);
        
        // If no candidates found, try with fewer keywords
        if (candidates.isEmpty() && projectKeys != null && projectKeys.length > 0) {
            keywords = extractKeywords(searchText, 3);
            searchJql = projectJql + " AND (summary ~ \"" + keywords.replace("\"", "\\\"") + "\")";
            logger.info("Retry with simpler JQL: {}", searchJql);
            candidates = fetchDetailedJiraIssuesByJql(searchJql, 100);
        }
        
        // Calculate similarity scores for each candidate
        List<Map<String, Object>> rankedResults = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            String candidateSummary = (String) candidate.get("summary");
            String candidateDescription = (String) candidate.get("description");
            
            // Calculate similarity score
            double similarityScore = similarityUtil.calculateIssueSimilarity(
                summary, description, candidateSummary, candidateDescription
            );
            
            // Only include results that meet the minimum similarity threshold
            if (similarityScore >= minSimilarity) {
                Map<String, Object> result = new HashMap<>(candidate);
                result.put("similarityScore", similarityScore);
                rankedResults.add(result);
            }
        }
        
        // Sort by similarity score (descending)
        rankedResults.sort((a, b) -> 
            Double.compare((Double)b.get("similarityScore"), (Double)a.get("similarityScore"))
        );
        
        // Return top results
        return rankedResults.stream().limit(maxResults).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> findPotentialDuplicateIssuesWithJql(String summary, String description, 
                                                                   String jqlQuery, double minSimilarity, int maxResults) {
        // Check if we have at least one non-empty field to search with
        if ((summary == null || summary.trim().isEmpty()) && 
            (description == null || description.trim().isEmpty())) {
            logger.warn("Both summary and description are empty - cannot perform similarity search");
            return Collections.emptyList();
        }
        
        logger.info("Finding potential duplicates using custom JQL: {}", jqlQuery);
        
        try {
            // Directly fetch candidates using the provided JQL
            List<Map<String, Object>> candidates = fetchDetailedJiraIssuesByJql(jqlQuery, 100);
            
            if (candidates.isEmpty()) {
                logger.info("No issues found matching the JQL query: {}", jqlQuery);
                return Collections.emptyList();
            }
            
            logger.info("Found {} candidate issues for similarity comparison", candidates.size());
            
            // Calculate similarity scores for each candidate
            List<Map<String, Object>> rankedResults = new ArrayList<>();
            for (Map<String, Object> candidate : candidates) {
                String candidateSummary = (String) candidate.get("summary");
                String candidateDescription = (String) candidate.get("description");
                
                // Calculate similarity score
                double similarityScore = similarityUtil.calculateIssueSimilarity(
                    summary, description, candidateSummary, candidateDescription
                );
                
                // Only include results that meet the minimum similarity threshold
                if (similarityScore >= minSimilarity) {
                    Map<String, Object> result = new HashMap<>(candidate);
                    result.put("similarityScore", similarityScore);
                    rankedResults.add(result);
                }
            }
            
            logger.info("Found {} potential duplicates after similarity filtering", rankedResults.size());
            
            // Sort by similarity score (descending)
            rankedResults.sort((a, b) -> 
                Double.compare((Double)b.get("similarityScore"), (Double)a.get("similarityScore"))
            );
            
            // Return top results
            return rankedResults.stream().limit(maxResults).collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error finding duplicates with JQL query: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Extract important keywords from the summary for JQL search
     */
    private String extractKeywords(String text, int maxKeywords) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Split by non-word characters
        String[] words = text.split("\\W+");
        
        // Filter out short words and common stop words
        Set<String> stopWords = Set.of("a", "an", "the", "and", "or", "but", "if", "then", "is", "are", 
            "to", "of", "for", "in", "on", "at", "by", "with", "about", "as");
            
        List<String> keywords = Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word.toLowerCase()))
            .limit(maxKeywords)
            .collect(Collectors.toList());
        
        return String.join(" ", keywords);
    }
    
    /**
     * Safely retrieves a nested value from a map with a default fallback
     */
    private Object getNestedValue(Map<String, Object> map, String key, Object defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return map.get(key);
    }
    
    @Override
    public List<Map<String, Object>> findPotentialDuplicatesWithSingleText(String text, String[] projectKeys,
                                                                         double minSimilarity, int maxResults) {
        // Check if we have a non-empty text to search with
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Input text is empty - cannot perform similarity search");
            return Collections.emptyList();
        }
        
        logger.info("Finding potential duplicates using single text input and project keys");
        
        // Use the same text for both summary and description comparison
        return findPotentialDuplicateIssues(text, text, projectKeys, minSimilarity, maxResults);
    }
    
    @Override
    public List<Map<String, Object>> findPotentialDuplicatesWithSingleTextAndJql(String text, String jqlQuery,
                                                                              double minSimilarity, int maxResults) {
        // Check if we have a non-empty text to search with
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Input text is empty - cannot perform similarity search");
            return Collections.emptyList();
        }
        
        logger.info("Finding potential duplicates using single text input and custom JQL");
        
        // Use the same text for both summary and description comparison
        return findPotentialDuplicateIssuesWithJql(text, text, jqlQuery, minSimilarity, maxResults);
    }
}