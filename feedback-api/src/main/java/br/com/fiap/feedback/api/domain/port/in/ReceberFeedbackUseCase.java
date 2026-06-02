package br.com.fiap.feedback.api.domain.port.in;

import br.com.fiap.feedback.api.domain.model.Feedback;

public interface ReceberFeedbackUseCase {

    Feedback receber(String descricao, int nota);
}
