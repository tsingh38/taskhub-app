package com.taskhub.taskservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

    private final BuildProperties build;

    public VersionController(BuildProperties build) {
        this.build = build;
    }
    public record VersionResponse(String version) {}

    @Operation(summary = "Service build version")
    @GetMapping("/version")
    public VersionResponse version() {
        return new VersionResponse(build.getVersion());
    }
}