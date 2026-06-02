package br.com.fiap.feedback.api.domain.port.out;

import br.com.fiap.feedback.api.domain.model.Feedback;

public interface PublicarFeedbackPort {

    void publicar(Feedback feedback);
}
