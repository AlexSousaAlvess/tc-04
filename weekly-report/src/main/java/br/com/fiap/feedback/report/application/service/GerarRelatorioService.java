package br.com.fiap.feedback.report.application.service;

import br.com.fiap.feedback.report.domain.model.FeedbackResumo;
import br.com.fiap.feedback.report.domain.model.RelatorioSemanal;
import br.com.fiap.feedback.report.domain.port.in.GerarRelatorioUseCase;
import br.com.fiap.feedback.report.domain.port.out.BuscarFeedbacksPort;
import br.com.fiap.feedback.report.domain.port.out.EnviarRelatorioPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GerarRelatorioService implements GerarRelatorioUseCase {

    @Inject
    BuscarFeedbacksPort buscarFeedbacksPort;

    @Inject
    EnviarRelatorioPort enviarRelatorioPort;

    @Override
    public RelatorioSemanal gerar(LocalDate dataInicio, LocalDate dataFim) {
        List<FeedbackResumo> feedbacks = buscarFeedbacksPort.buscarPorPeriodo(dataInicio, dataFim);
        RelatorioSemanal relatorio = new RelatorioSemanal(dataInicio, dataFim, feedbacks);

        Log.infof("Relatório gerado: %d feedbacks de %s a %s, média=%.2f",
            relatorio.getTotalFeedbacks(), dataInicio, dataFim, relatorio.getMediaNotas());

        enviarRelatorioPort.enviar(relatorio);
        return relatorio;
    }
}
