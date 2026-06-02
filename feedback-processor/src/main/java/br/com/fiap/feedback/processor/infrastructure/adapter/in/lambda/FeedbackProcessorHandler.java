package br.com.fiap.feedback.processor.infrastructure.adapter.in.lambda;

import br.com.fiap.feedback.processor.domain.model.Feedback;
import br.com.fiap.feedback.processor.domain.port.in.ProcessarFeedbackUseCase;
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

@Named("processor-handler")
@ApplicationScoped
public class FeedbackProcessorHandler implements RequestHandler<SQSEvent, String> {

    @Inject
    ProcessarFeedbackUseCase processarFeedbackUseCase;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        int processados = 0;
        int falhas = 0;

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                JsonNode node = objectMapper.readTree(message.getBody());

                Feedback feedback = new Feedback(
                    node.get("id").asText(),
                    node.get("descricao").asText(),
                    node.get("nota").asInt(),
                    LocalDateTime.parse(node.get("criadoEm").asText())
                );

                processarFeedbackUseCase.processar(feedback);
                processados++;
            } catch (Exception e) {
                Log.errorf(e, "Erro ao processar mensagem SQS: messageId=%s", message.getMessageId());
                falhas++;
            }
        }

        Log.infof("Processamento concluído: %d processados, %d falhas", processados, falhas);

        if (falhas > 0) {
            throw new RuntimeException("Falha ao processar " + falhas + " mensagem(ns)");
        }
        return "OK";
    }
}
