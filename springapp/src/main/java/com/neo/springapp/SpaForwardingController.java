package com.neo.springapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {
    
    // Forward everything except paths containing a dot (.) or starting with /api
    @RequestMapping(value = { "/", "/{path:[^.]*}", "/{path:[^.]*}/**" })
    public String forward() {
        return "forward:/index.html";
    }
}