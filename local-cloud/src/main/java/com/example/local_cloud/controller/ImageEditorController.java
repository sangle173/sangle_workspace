package com.example.local_cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImageEditorController {

    @GetMapping("/image-editor")
    public String imageEditor() {
        return "image-editor";
    }
} 