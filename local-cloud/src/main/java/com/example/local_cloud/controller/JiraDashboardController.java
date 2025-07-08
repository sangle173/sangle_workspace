package com.example.local_cloud.controller;

import com.example.local_cloud.service.JiraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/jira-dashboard")
public class JiraDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(JiraDashboardController.class);

    @Autowired
    private JiraService jiraService;

    @GetMapping
    public String showDashboard(Model model) {
        return "jira_dashboard";
    }

    @PostMapping("/fetch-issues")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fetchIssues(@RequestParam("issueKeys") String issueKeys) {
        List<Map<String, Object>> issues = jiraService.fetchJiraIssues(issueKeys.trim().split("\\s+"));
        return ResponseEntity.ok(Map.of("issues", issues));
    }
    
    @GetMapping("/tasks")
    public String showTasksPage(Model model) {
        // Add the filters to the model
        model.addAttribute("jiraFilters", jiraService.getJiraFilters());
        return "jira_tasks";
    }
    
    @GetMapping("/filter/{filterName}")
    public String showFilterResults(@PathVariable String filterName, Model model, HttpServletRequest request) {
        logger.info("Endpoint called: {} {}", request.getMethod(), request.getRequestURI());
        logger.info("Full URL: {}", request.getRequestURL());
        logger.info("Accessing Jira filter: {}", filterName);
        
        String jql = jiraService.getJiraFilterByName(filterName);
        if (jql == null) {
            logger.warn("Filter not found: {}", filterName);
            model.addAttribute("error", "Filter not found: " + filterName);
            return "jira_filter_detail";
        }
        
        logger.info("Filter '{}' resolved to JQL: {}", filterName, jql);
        model.addAttribute("filterName", filterName);
        model.addAttribute("jql", jql);
        return "jira_filter_detail";
    }
    
    @PostMapping("/fetch-by-jql")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fetchIssuesByJql(
            @RequestParam("jql") String jql,
            @RequestParam(value = "maxResults", required = false, defaultValue = "500") int maxResults,
            HttpServletRequest request) {
        
        logger.info("Endpoint called: {} {}", request.getMethod(), request.getRequestURI());
        logger.info("Full URL: {}", request.getRequestURL() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
        logger.info("Fetching Jira issues with JQL: {}", jql);
        logger.info("Max results requested: {}, applying limit: {}", maxResults, Math.min(maxResults, 1000));
        
        // Use the maxResults parameter with a reasonable upper limit to avoid overloading
        int limit = Math.min(maxResults, 1000); // Cap at 1000 issues maximum
        
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> issues = jiraService.fetchJiraIssuesByJql(jql, limit);
        long endTime = System.currentTimeMillis();
        
        logger.info("Jira fetch completed in {} ms, returned {} issues", (endTime - startTime), issues.size());
        
        // Add total count for UI feedback
        Map<String, Object> response = new HashMap<>();
        response.put("issues", issues);
        response.put("totalCount", issues.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/filters")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getFilters() {
        return ResponseEntity.ok(jiraService.getJiraFilters());
    }
    
    @GetMapping("/duplicate-checker")
    public String showDuplicateCheckerPage(Model model) {
        // Add default projects for duplicate checking
        model.addAttribute("defaultProjects", "DUKE,RND,PMA");
        return "jira_duplicate_checker";
    }
    
    @PostMapping("/check-duplicates")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkDuplicates(
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "useCustomJql", required = false, defaultValue = "false") boolean useCustomJql,
            @RequestParam(value = "projects", required = false, defaultValue = "DUKE,RND,PMA") String projects,
            @RequestParam(value = "jqlQuery", required = false) String jqlQuery,
            @RequestParam(value = "minSimilarity", required = false, defaultValue = "0.3") double minSimilarity,
            @RequestParam(value = "maxResults", required = false, defaultValue = "10") int maxResults,
            HttpServletRequest request) {
        
        logger.info("Endpoint called: {} {}", request.getMethod(), request.getRequestURI());
        
        // Validate that we have at least one search field
        if ((summary == null || summary.trim().isEmpty()) && 
            (description == null || description.trim().isEmpty())) {
            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "error", "At least one of summary or description must be provided", 
                    "potentialDuplicates", Collections.emptyList(), 
                    "totalCount", 0
                ));
        }
        
        logger.info("Checking for duplicates with summary: {}, description length: {}", 
            summary, 
            (description != null) ? description.length() : 0);
        
        List<Map<String, Object>> potentialDuplicates;
        long startTime = System.currentTimeMillis();
        
        if (useCustomJql && jqlQuery != null && !jqlQuery.trim().isEmpty()) {
            // Use custom JQL query to find duplicates
            logger.info("Using custom JQL query: {}", jqlQuery);
            potentialDuplicates = jiraService.findPotentialDuplicateIssuesWithJql(
                summary, description, jqlQuery, minSimilarity, maxResults
            );
        } else {
            // Use project-based search
            logger.info("Using project-based search with projects: {}", projects);
            // Parse project keys
            String[] projectKeys = projects.split("\\s*,\\s*");
            potentialDuplicates = jiraService.findPotentialDuplicateIssues(
                summary, description, projectKeys, minSimilarity, maxResults
            );
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("Duplicate check completed in {} ms, found {} potential duplicates", 
                   (endTime - startTime), potentialDuplicates.size());
        
        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("potentialDuplicates", potentialDuplicates);
        response.put("totalCount", potentialDuplicates.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/check-duplicates-single")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkDuplicatesSingleText(
            @RequestParam(value = "text", required = true) String text,
            @RequestParam(value = "useCustomJql", required = false, defaultValue = "false") boolean useCustomJql,
            @RequestParam(value = "projects", required = false, defaultValue = "DUKE,RND,PMA") String projects,
            @RequestParam(value = "jqlQuery", required = false) String jqlQuery,
            @RequestParam(value = "minSimilarity", required = false, defaultValue = "0.3") double minSimilarity,
            @RequestParam(value = "maxResults", required = false, defaultValue = "10") int maxResults,
            HttpServletRequest request) {
        
        logger.info("Endpoint called: {} {}", request.getMethod(), request.getRequestURI());
        
        // Validate that we have text to search with
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "error", "Search text must be provided", 
                    "potentialDuplicates", Collections.emptyList(), 
                    "totalCount", 0
                ));
        }
        
        logger.info("Checking for duplicates with single text input, length: {}", text.length());
        
        List<Map<String, Object>> potentialDuplicates;
        long startTime = System.currentTimeMillis();
        
        if (useCustomJql && jqlQuery != null && !jqlQuery.trim().isEmpty()) {
            // Use custom JQL query to find duplicates
            logger.info("Using custom JQL query: {}", jqlQuery);
            potentialDuplicates = jiraService.findPotentialDuplicatesWithSingleTextAndJql(
                text, jqlQuery, minSimilarity, maxResults
            );
        } else {
            // Use project-based search
            logger.info("Using project-based search with projects: {}", projects);
            // Parse project keys
            String[] projectKeys = projects.split("\\s*,\\s*");
            potentialDuplicates = jiraService.findPotentialDuplicatesWithSingleText(
                text, projectKeys, minSimilarity, maxResults
            );
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("Duplicate check with single text completed in {} ms, found {} potential duplicates", 
                   (endTime - startTime), potentialDuplicates.size());
        
        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("potentialDuplicates", potentialDuplicates);
        response.put("totalCount", potentialDuplicates.size());
        
        return ResponseEntity.ok(response);
    }
}