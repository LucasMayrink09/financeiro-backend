package com.gestao.financeira.dto;

import java.util.List;

public record GeminiResponseDTO(List<Candidate> candidates) {
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    public String getText() {
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).content().parts().get(0).text();
        }
        return "Não foi possível gerar uma análise no momento.";
    }
}