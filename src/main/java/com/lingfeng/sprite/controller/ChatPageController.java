package com.lingfeng.sprite.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Life Frontend Controller
 *
 * Routes the server-rendered entry points for the life frontend shell.
 */
@Controller
public class ChatPageController {

    @GetMapping("/chat")
    public String chat() {
        return "forward:/chat/index.html";
    }

    @GetMapping("/memory")
    public String memory() {
        return "forward:/memory/index.html";
    }

    @GetMapping("/settings")
    public String settings() {
        return "forward:/settings/index.html";
    }
}
