package com.diddycart.modules.payment.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ViewController {

    // Serve "static/payment.html" as HTML by "/checkout"
    @GetMapping(value = "/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public Resource checkoutPage() {
        return new ClassPathResource("static/payment.html");
    }

    // Serve "static/payment-success.html" as HTML by "/payment-success"
    @GetMapping(value = "/payment-success", produces = MediaType.TEXT_HTML_VALUE)
    public Resource successPage() {
        return new ClassPathResource("static/payment-success.html");
    }

    // Serve "static/payment-failure.html" as HTML by "/payment-failure"
    @GetMapping(value = "/payment-failure", produces = MediaType.TEXT_HTML_VALUE)
    public Resource failurePage() {
        return new ClassPathResource("static/payment-failure.html");
    }
}