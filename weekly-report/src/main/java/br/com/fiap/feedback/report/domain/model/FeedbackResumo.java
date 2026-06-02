package br.com.fiap.feedback.report.domain.model;

import java.time.LocalDate;

public record FeedbackResumo(
    String descricao,
    int nota,
    String urgencia,
    LocalDate dataEnvio
) {}
