package br.com.fiap.feedback.api.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Feedback {

    private final String id;
    private final String descricao;
    private final int nota;
    private final Urgencia urgencia;
    private final LocalDateTime criadoEm;

    public Feedback(String descricao, int nota) {
        this.id = UUID.randomUUID().toString();
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = Urgencia.calcular(nota);
        this.criadoEm = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getDescricao() { return descricao; }
    public int getNota() { return nota; }
    public Urgencia getUrgencia() { return urgencia; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
