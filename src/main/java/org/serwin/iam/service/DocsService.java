package org.serwin.iam.service;

import java.io.InputStream;
import java.util.Map;

import org.serwin.iam.dto.DocResponse;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocsService {

    private static final String BASE_PATH = "docs/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object getManifest() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(BASE_PATH + "manifest.json")) {

            return objectMapper.readValue(is, Object.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load manifest", e);
        }
    }

    public DocResponse getDoc(String slug) {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(BASE_PATH + slug + ".md")) {

            if (is == null) {
                throw new RuntimeException("Doc not found: " + slug);
            }

            String raw = new String(is.readAllBytes());

            return parseMarkdown(raw);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load doc: " + slug, e);
        }
    }
    private DocResponse parseMarkdown(String raw) {
    if (!raw.startsWith("---")) {
        return new DocResponse(Map.of(), raw);
    }

    String[] parts = raw.split("---", 3);

    String metaRaw = parts[1];
    String content = parts[2];

    Map<String, Object> metadata = parseYaml(metaRaw);

    return new DocResponse(metadata, content.trim());
}

private Map<String, Object> parseYaml(String yaml) {
    Yaml parser = new Yaml();
    return parser.load(yaml);
}
}
