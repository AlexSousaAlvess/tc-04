package br.com.fiap.feedback.processor.application.service;

import br.com.fiap.feedback.processor.domain.model.Feedback;
import br.com.fiap.feedback.processor.domain.port.in.ProcessarFeedbackUseCase;
import br.com.fiap.feedback.processor.domain.port.out.PublicarNotificacaoPort;
import br.com.fiap.feedback.processor.domain.port.out.SalvarFeedbackPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProcessarFeedbackService implements ProcessarFeedbackUseCase {

    @Inject
    SalvarFeedbackPort salvarFeedbackPort;

    @Inject
    PublicarNotificacaoPort publicarNotificacaoPort;

    @Override
    @Transactional
    public void processar(Feedback feedback) {
        salvarFeedbackPort.salvar(feedback);
        Log.infof("Feedback salvo: id=%s urgencia=%s", feedback.getId(), feedback.getUrgencia());

        if (feedback.getUrgencia().isCritica()) {
            publicarNotificacaoPort.publicar(feedback);
            Log.infof("Notificação urgente publicada: id=%s nota=%d", feedback.getId(), feedback.getNota());
        }
    }
}
