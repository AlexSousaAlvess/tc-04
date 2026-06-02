package br.com.fiap.feedback.report.infrastructure.adapter.out.repository;

import br.com.fiap.feedback.report.domain.model.FeedbackResumo;
import br.com.fiap.feedback.report.domain.port.out.BuscarFeedbacksPort;
import br.com.fiap.feedback.report.infrastructure.adapter.out.repository.entity.FeedbackEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class FeedbackReportRepository implements BuscarFeedbacksPort {

    @Override
    public List<FeedbackResumo> buscarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<FeedbackEntity> entities = FeedbackEntity.find(
            "criadoEm >= ?1 AND criadoEm <= ?2 ORDER BY criadoEm ASC",
            inicio, fim
        ).list();

        return entities.stream()
            .map(e -> new FeedbackResumo(
                e.descricao,
                e.nota,
                e.urgencia,
                e.criadoEm.toLocalDate()
            ))
            .toList();
    }
}
