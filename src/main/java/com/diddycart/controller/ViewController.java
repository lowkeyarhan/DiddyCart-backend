package com.diddycart.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ViewController {

    // Map "/checkout" -> Serve "static/payment.html" as HTML
    @GetMapping(value = "/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public Resource checkoutPage() {
        return new ClassPathResource("static/payment.html");
    }

    // Serve Success Page
    @GetMapping(value = "/payment-success", produces = MediaType.TEXT_HTML_VALUE)
    public Resource successPage() {
        return new ClassPathResource("static/payment-success.html");
    }

    // Serve Failure Page
    @GetMapping(value = "/payment-failure", produces = MediaType.TEXT_HTML_VALUE)
    public Resource failurePage() {
        return new ClassPathResource("static/payment-failure.html");
    }
}