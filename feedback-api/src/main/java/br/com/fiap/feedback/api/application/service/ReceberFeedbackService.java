package br.com.fiap.feedback.api.application.service;

import br.com.fiap.feedback.api.domain.model.Feedback;
import br.com.fiap.feedback.api.domain.port.in.ReceberFeedbackUseCase;
import br.com.fiap.feedback.api.domain.port.out.PublicarFeedbackPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReceberFeedbackService implements ReceberFeedbackUseCase {

    @Inject
    PublicarFeedbackPort publicarFeedbackPort;

    @Override
    public Feedback receber(String descricao, int nota) {
        validar(descricao, nota);
        Feedback feedback = new Feedback(descricao, nota);
        publicarFeedbackPort.publicar(feedback);
        return feedback;
    }

    private void validar(String descricao, int nota) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição não pode ser vazia");
        }
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Nota deve estar entre 0 e 10");
        }
    }
}
