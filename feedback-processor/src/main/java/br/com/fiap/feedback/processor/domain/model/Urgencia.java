package br.com.fiap.feedback.processor.domain.model;

public enum Urgencia {

    CRITICA,
    MEDIA,
    BAIXA;

    public static Urgencia calcular(int nota) {
        if (nota <= 3) return CRITICA;
        if (nota <= 6) return MEDIA;
        return BAIXA;
    }

    public boolean isCritica() {
        return this == CRITICA;
    }
}
