package br.com.fiap.feedback.processor.domain.model;

import java.time.LocalDateTime;

public class Feedback {

    private String id;
    private String descricao;
    private int nota;
    private Urgencia urgencia;
    private LocalDateTime criadoEm;

    public Feedback(String id, String descricao, int nota, LocalDateTime criadoEm) {
        this.id = id;
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = Urgencia.calcular(nota);
        this.criadoEm = criadoEm;
    }

    public String getId() { return id; }
    public String getDescricao() { return descricao; }
    public int getNota() { return nota; }
    public Urgencia getUrgencia() { return urgencia; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
