package br.com.fiap.feedback.processor.domain.port.out;

import br.com.fiap.feedback.processor.domain.model.Feedback;

public interface SalvarFeedbackPort {

    void salvar(Feedback feedback);
}
