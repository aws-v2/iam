package org.serwin.iam.web;

import java.util.Map;

import org.serwin.iam.service.DocsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/docs")
public class DocsController {

    private final DocsService docsService;

    public DocsController(DocsService docsService) {
        this.docsService = docsService;
    }

    @GetMapping
    public ResponseEntity<?> getManifest() {
        return ResponseEntity.ok(Map.of(
            "data", docsService.getManifest()
        ));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getDoc(@PathVariable String slug) {
        return ResponseEntity.ok(Map.of(
            "data", docsService.getDoc(slug)
        ));
    }
}