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
} 