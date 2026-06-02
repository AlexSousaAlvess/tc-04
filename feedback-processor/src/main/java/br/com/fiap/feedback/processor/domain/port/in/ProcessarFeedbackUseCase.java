package br.com.fiap.feedback.processor.domain.port.in;

import br.com.fiap.feedback.processor.domain.model.Feedback;

public interface ProcessarFeedbackUseCase {

    void processar(Feedback feedback);
}
