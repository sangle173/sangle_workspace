package com.example.local_cloud.controller;

import com.example.local_cloud.service.JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/jira-dashboard")
public class JiraDashboardController {

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
    public String showFilterResults(@PathVariable String filterName, Model model) {
        String jql = jiraService.getJiraFilterByName(filterName);
        if (jql == null) {
            model.addAttribute("error", "Filter not found: " + filterName);
            return "jira_filter_detail";
        }
        
        model.addAttribute("filterName", filterName);
        model.addAttribute("jql", jql);
        return "jira_filter_detail";
    }
    
    @PostMapping("/fetch-by-jql")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fetchIssuesByJql(@RequestParam("jql") String jql) {
        List<Map<String, Object>> issues = jiraService.fetchJiraIssuesByJql(jql);
        return ResponseEntity.ok(Map.of("issues", issues));
    }
    
    @GetMapping("/filters")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getFilters() {
        return ResponseEntity.ok(jiraService.getJiraFilters());
    }
} 