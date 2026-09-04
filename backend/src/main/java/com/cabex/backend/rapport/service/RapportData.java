package com.cabex.backend.rapport.service;

import java.util.List;

public record RapportData(
        String titre,
        List<String> enTetes,
        List<List<Object>> lignes
) {
}
