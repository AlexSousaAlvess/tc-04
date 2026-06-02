package br.com.fiap.feedback.processor.domain.port.out;

import br.com.fiap.feedback.processor.domain.model.Feedback;

public interface PublicarNotificacaoPort {

    void publicar(Feedback feedback);
}
