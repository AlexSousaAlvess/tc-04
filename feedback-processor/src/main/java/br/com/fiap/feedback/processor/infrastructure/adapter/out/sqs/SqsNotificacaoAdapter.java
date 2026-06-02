package br.com.fiap.feedback.processor.infrastructure.adapter.out.sqs;

import br.com.fiap.feedback.processor.domain.model.Feedback;
import br.com.fiap.feedback.processor.domain.port.out.PublicarNotificacaoPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@ApplicationScoped
public class SqsNotificacaoAdapter implements PublicarNotificacaoPort {

    @Inject
    SqsClient sqsClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.sqs.notification-queue-url")
    String notificationQueueUrl;

    @Override
    public void publicar(Feedback feedback) {
        try {
            String mensagem = objectMapper.writeValueAsString(Map.of(
                "id", feedback.getId(),
                "descricao", feedback.getDescricao(),
                "nota", feedback.getNota(),
                "urgencia", feedback.getUrgencia().name(),
                "criadoEm", feedback.getCriadoEm().toString()
            ));

            sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(notificationQueueUrl)
                .messageBody(mensagem)
                .messageGroupId("notification")
                .messageDeduplicationId(feedback.getId() + "-notification")
                .build());

            Log.infof("Notificação urgente publicada na fila: id=%s", feedback.getId());
        } catch (Exception e) {
            Log.errorf(e, "Erro ao publicar notificação na fila SQS");
            throw new RuntimeException("Falha ao publicar notificação", e);
        }
    }
}
