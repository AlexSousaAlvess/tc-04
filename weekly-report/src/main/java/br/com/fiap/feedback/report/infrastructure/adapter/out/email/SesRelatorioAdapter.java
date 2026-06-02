package br.com.fiap.feedback.report.infrastructure.adapter.out.email;

import br.com.fiap.feedback.report.domain.model.FeedbackResumo;
import br.com.fiap.feedback.report.domain.model.RelatorioSemanal;
import br.com.fiap.feedback.report.domain.port.out.EnviarRelatorioPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SesRelatorioAdapter implements EnviarRelatorioPort {

    @Inject
    SesClient sesClient;

    @ConfigProperty(name = "app.admin.email")
    String adminEmail;

    @ConfigProperty(name = "app.ses.sender-email")
    String senderEmail;

    @Override
    public void enviar(RelatorioSemanal relatorio) {
        String assunto = String.format("[FIAP] Relatório Semanal de Feedbacks - %s a %s",
            relatorio.getDataInicio(), relatorio.getDataFim());
        String corpo = montarCorpoEmail(relatorio);

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder().toAddresses(adminEmail).build())
                .message(Message.builder()
                    .subject(Content.builder().data(assunto).charset("UTF-8").build())
                    .body(Body.builder()
                        .html(Content.builder().data(corpo).charset("UTF-8").build())
                        .build())
                    .build())
                .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            Log.infof("Relatório semanal enviado: messageId=%s", response.messageId());
        } catch (Exception e) {
            Log.errorf(e, "Erro ao enviar relatório semanal via SES");
            throw new RuntimeException("Falha ao enviar relatório", e);
        }
    }

    private String montarCorpoEmail(RelatorioSemanal relatorio) {
        StringBuilder tabelaFeedbacks = new StringBuilder();
        for (FeedbackResumo f : relatorio.getFeedbacks()) {
            tabelaFeedbacks.append(String.format(
                "<tr><td>%s</td><td>%d</td><td>%s</td><td>%s</td></tr>",
                f.descricao(), f.nota(), f.urgencia(), f.dataEnvio()
            ));
        }

        StringBuilder porDia = new StringBuilder();
        relatorio.getQuantidadePorDia().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> porDia.append(String.format("<tr><td>%s</td><td>%d</td></tr>", e.getKey(), e.getValue())));

        StringBuilder porUrgencia = new StringBuilder();
        relatorio.getQuantidadePorUrgencia()
            .forEach((k, v) -> porUrgencia.append(String.format("<tr><td>%s</td><td>%d</td></tr>", k, v)));

        return String.format("""
            <html>
            <body>
                <h2>Relatório Semanal de Feedbacks</h2>
                <p><strong>Período:</strong> %s a %s</p>
                <p><strong>Total de feedbacks:</strong> %d</p>
                <p><strong>Média das notas:</strong> %.2f / 10</p>

                <h3>Feedbacks por Dia</h3>
                <table border="1" cellpadding="5">
                    <tr><th>Data</th><th>Quantidade</th></tr>
                    %s
                </table>

                <h3>Feedbacks por Urgência</h3>
                <table border="1" cellpadding="5">
                    <tr><th>Urgência</th><th>Quantidade</th></tr>
                    %s
                </table>

                <h3>Detalhamento</h3>
                <table border="1" cellpadding="5">
                    <tr><th>Descrição</th><th>Nota</th><th>Urgência</th><th>Data</th></tr>
                    %s
                </table>
            </body>
            </html>
            """,
            relatorio.getDataInicio(), relatorio.getDataFim(),
            relatorio.getTotalFeedbacks(),
            relatorio.getMediaNotas(),
            porDia,
            porUrgencia,
            tabelaFeedbacks
        );
    }
}
