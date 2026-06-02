package br.com.fiap.feedback.report.infrastructure.adapter.in.lambda;

import br.com.fiap.feedback.report.domain.model.RelatorioSemanal;
import br.com.fiap.feedback.report.domain.port.in.GerarRelatorioUseCase;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.LocalDate;
import java.util.Map;

@Named("report-handler")
@ApplicationScoped
public class WeeklyReportHandler implements RequestHandler<Map<String, Object>, String> {

    @Inject
    GerarRelatorioUseCase gerarRelatorioUseCase;

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        try {
            LocalDate hoje = LocalDate.now();
            LocalDate dataInicio = hoje.minusDays(7);
            LocalDate dataFim = hoje.minusDays(1);

            Log.infof("Gerando relatório semanal de %s a %s", dataInicio, dataFim);

            RelatorioSemanal relatorio = gerarRelatorioUseCase.gerar(dataInicio, dataFim);

            Log.infof("Relatório semanal gerado e enviado: %d feedbacks, média=%.2f",
                relatorio.getTotalFeedbacks(), relatorio.getMediaNotas());

            return String.format("Relatório enviado: %d feedbacks processados", relatorio.getTotalFeedbacks());
        } catch (Exception e) {
            Log.errorf(e, "Erro ao gerar relatório semanal");
            throw new RuntimeException("Falha ao gerar relatório semanal", e);
        }
    }
}
