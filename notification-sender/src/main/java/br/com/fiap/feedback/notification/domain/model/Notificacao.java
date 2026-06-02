package br.com.fiap.feedback.notification.domain.model;

import java.time.LocalDateTime;

public class Notificacao {

    private final String feedbackId;
    private final String descricao;
    private final int nota;
    private final String urgencia;
    private final LocalDateTime dataEnvio;

    public Notificacao(String feedbackId, String descricao, int nota, String urgencia, LocalDateTime dataEnvio) {
        this.feedbackId = feedbackId;
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = urgencia;
        this.dataEnvio = dataEnvio;
    }

    public String getFeedbackId() { return feedbackId; }
    public String getDescricao() { return descricao; }
    public int getNota() { return nota; }
    public String getUrgencia() { return urgencia; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
}
