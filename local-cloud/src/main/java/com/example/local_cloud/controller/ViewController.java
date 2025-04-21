package com.example.local_cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/sonos")
    public String sonosPage() {
        return "sonos"; // returns templates/sonos.html
    }

    @GetMapping("/url-converter")
    public String showUrlConverter() {
        return "url_converter";
    }
}
