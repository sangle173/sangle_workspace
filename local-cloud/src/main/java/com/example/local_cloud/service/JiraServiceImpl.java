package com.example.local_cloud.service;

import com.example.local_cloud.config.EnvConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

@Service
public class JiraServiceImpl implements JiraService {

    private static final Logger logger = LoggerFactory.getLogger(JiraServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final EnvConfig envConfig;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${jira.url:https://jira.sonos.com}")
    private String jiraUrl;
    
    // Constants for environment variable keys
    private static final String JIRA_TOKEN_ENV_KEY = "JIRA_API_TOKEN";
    // Path to the JIRA filter JSON file
    private static final String JIRA_FILTER_JSON_PATH = System.getProperty("user.home") + "/Desktop/Env/jira_filter.json";
    
    @Autowired
    public JiraServiceImpl(EnvConfig envConfig, ResourceLoader resourceLoader) {
        this.envConfig = envConfig;
        this.resourceLoader = resourceLoader;
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
        List<Map<String, Object>> issuesData = new ArrayList<>();
        
        // Fetch token from .env file
        String jiraToken = envConfig.getEnvValue(JIRA_TOKEN_ENV_KEY, "");
        if (jiraToken.isEmpty()) {
            logger.warn("JIRA API token not found in .env file. Please add '{}=your_token' to {}/Desktop/Env/.env",
                    JIRA_TOKEN_ENV_KEY, System.getProperty("user.home"));
            return issuesData; // Return empty list if no token
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jiraToken);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Build URL with JQL parameter
            String url = UriComponentsBuilder
                .fromUriString(jiraUrl + "/rest/api/latest/search")
                .queryParam("jql", jql)
                .queryParam("maxResults", 50)
                .build()
                .toUriString();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            
            Map<String, Object> searchResult = response.getBody();
            
            if (searchResult != null && searchResult.containsKey("issues")) {
                List<Map<String, Object>> issues = (List<Map<String, Object>>) searchResult.get("issues");
                
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
            }
            
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
    
    /**
     * Safely retrieves a nested value from a map with a default fallback
     */
    private Object getNestedValue(Map<String, Object> map, String key, Object defaultValue) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }
        return map.get(key);
    }
} 