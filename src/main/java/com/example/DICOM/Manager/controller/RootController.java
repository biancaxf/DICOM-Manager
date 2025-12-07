package com.example.DICOM.Manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
    @GetMapping({"/", "/home"})
    public String root() {
        return "redirect:/dicom/";
    }
}
