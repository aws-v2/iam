package org.serwin.iam.web;

import java.util.Map;

import org.serwin.iam.domain.DocType;
import org.serwin.iam.service.DocsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/docs")
public class PublicDocsController {

    private final DocsService docsService;

    public PublicDocsController(DocsService docsService) {
        this.docsService = docsService;
    }



    @GetMapping
    public ResponseEntity<?> getManifest() {
        return ResponseEntity.ok(
                Map.of("data", docsService.getManifest(DocType.PUBLIC)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getDoc(@PathVariable String slug) {
        return ResponseEntity.ok(
                Map.of("data", docsService.getDoc(DocType.PUBLIC, slug)));
    }
}