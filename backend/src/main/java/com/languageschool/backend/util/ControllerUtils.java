package com.languageschool.backend.util;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

public final class ControllerUtils {
    private ControllerUtils() {
    }

    public static <T> ResponseEntity<T> createdAtId(Object id, T body) {
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri();
        return ResponseEntity.created(uri).body(body);
    }
}
