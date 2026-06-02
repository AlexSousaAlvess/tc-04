package br.com.fiap.feedback.api.infrastructure.adapter.out.sqs;

import br.com.fiap.feedback.api.domain.model.Feedback;
import br.com.fiap.feedback.api.domain.port.out.PublicarFeedbackPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@ApplicationScoped
public class SqsPublicarFeedbackAdapter implements PublicarFeedbackPort {

    @Inject
    SqsClient sqsClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "app.sqs.feedback-queue-url")
    String feedbackQueueUrl;

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
                .queueUrl(feedbackQueueUrl)
                .messageBody(mensagem)
                .messageGroupId("feedback")
                .messageDeduplicationId(feedback.getId())
                .build());

            Log.infof("Feedback publicado na fila: id=%s", feedback.getId());
        } catch (Exception e) {
            Log.errorf(e, "Erro ao publicar feedback na fila SQS");
            throw new RuntimeException("Falha ao publicar feedback na fila", e);
        }
    }
}
