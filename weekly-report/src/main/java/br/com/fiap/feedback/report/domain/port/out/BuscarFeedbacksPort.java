package br.com.fiap.feedback.report.domain.port.out;

import br.com.fiap.feedback.report.domain.model.FeedbackResumo;

import java.time.LocalDate;
import java.util.List;

public interface BuscarFeedbacksPort {

    List<FeedbackResumo> buscarPorPeriodo(LocalDate dataInicio, LocalDate dataFim);
}
