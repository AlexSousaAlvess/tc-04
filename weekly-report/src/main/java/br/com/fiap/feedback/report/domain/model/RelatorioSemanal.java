package br.com.fiap.feedback.report.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelatorioSemanal {

    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final List<FeedbackResumo> feedbacks;

    public RelatorioSemanal(LocalDate dataInicio, LocalDate dataFim, List<FeedbackResumo> feedbacks) {
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.feedbacks = feedbacks;
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public List<FeedbackResumo> getFeedbacks() { return feedbacks; }
    public int getTotalFeedbacks() { return feedbacks.size(); }

    public double getMediaNotas() {
        return feedbacks.stream()
            .mapToInt(FeedbackResumo::nota)
            .average()
            .orElse(0.0);
    }

    public Map<LocalDate, Long> getQuantidadePorDia() {
        return feedbacks.stream()
            .collect(Collectors.groupingBy(FeedbackResumo::dataEnvio, Collectors.counting()));
    }

    public Map<String, Long> getQuantidadePorUrgencia() {
        return feedbacks.stream()
            .collect(Collectors.groupingBy(FeedbackResumo::urgencia, Collectors.counting()));
    }
}
