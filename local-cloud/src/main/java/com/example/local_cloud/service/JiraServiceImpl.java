package com.example.local_cloud.service;

import com.example.local_cloud.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class JiraServiceImpl implements JiraService {

    private static final Logger logger = LoggerFactory.getLogger(JiraServiceImpl.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final EnvConfig envConfig;
    
    @Value("${jira.url:https://jira.sonos.com}")
    private String jiraUrl;
    
    // Constants for environment variable keys
    private static final String JIRA_TOKEN_ENV_KEY = "JIRA_API_TOKEN";
    
    @Autowired
    public JiraServiceImpl(EnvConfig envConfig) {
        this.envConfig = envConfig;
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
                    
                    // Priority
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
                    
                    // QA Contact - using a placeholder field ID, update with your actual field
                    Map<String, Object> qaContact = (Map<String, Object>) fields.get("customfield_12345");
                    issueData.put("qa_contact", qaContact != null ? qaContact.get("displayName") : "Not Set");
                    
                    // Labels
                    List<String> labels = (List<String>) fields.get("labels");
                    issueData.put("labels", labels != null && !labels.isEmpty() ? String.join(", ", labels) : "None");
                    
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
                
                issuesData.add(errorData);
            }
        }
        
        return issuesData;
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