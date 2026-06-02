package br.com.fiap.feedback.processor.infrastructure.adapter.out.repository;

import br.com.fiap.feedback.processor.domain.model.Feedback;
import br.com.fiap.feedback.processor.domain.port.out.SalvarFeedbackPort;
import br.com.fiap.feedback.processor.infrastructure.adapter.out.repository.entity.FeedbackEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;

@ApplicationScoped
public class FeedbackRepository implements SalvarFeedbackPort {

    @Override
    public void salvar(Feedback feedback) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.feedbackId = feedback.getId();
        entity.descricao = feedback.getDescricao();
        entity.nota = feedback.getNota();
        entity.urgencia = feedback.getUrgencia().name();
        entity.criadoEm = feedback.getCriadoEm();
        entity.processadoEm = LocalDateTime.now();
        entity.persist();
    }
}
