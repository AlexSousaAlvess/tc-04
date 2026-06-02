package br.com.fiap.feedback.notification.infrastructure.adapter.in.lambda;

import br.com.fiap.feedback.notification.domain.model.Notificacao;
import br.com.fiap.feedback.notification.domain.port.in.EnviarNotificacaoUseCase;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.LocalDateTime;

@Named("notification-handler")
@ApplicationScoped
public class NotificationHandler implements RequestHandler<SQSEvent, String> {

    @Inject
    EnviarNotificacaoUseCase enviarNotificacaoUseCase;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        int enviados = 0;
        int falhas = 0;

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                JsonNode node = objectMapper.readTree(message.getBody());

                Notificacao notificacao = new Notificacao(
                    node.get("id").asText(),
                    node.get("descricao").asText(),
                    node.get("nota").asInt(),
                    node.get("urgencia").asText(),
                    LocalDateTime.parse(node.get("criadoEm").asText())
                );

                enviarNotificacaoUseCase.enviar(notificacao);
                enviados++;
            } catch (Exception e) {
                Log.errorf(e, "Erro ao enviar notificação: messageId=%s", message.getMessageId());
                falhas++;
            }
        }

        Log.infof("Notificações: %d enviadas, %d falhas", enviados, falhas);

        if (falhas > 0) {
            throw new RuntimeException("Falha ao enviar " + falhas + " notificação(ões)");
        }
        return "OK";
    }
}
