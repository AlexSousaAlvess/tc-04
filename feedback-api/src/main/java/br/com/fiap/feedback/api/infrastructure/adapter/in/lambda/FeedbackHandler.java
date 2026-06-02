package br.com.fiap.feedback.api.infrastructure.adapter.in.lambda;

import br.com.fiap.feedback.api.domain.model.Feedback;
import br.com.fiap.feedback.api.domain.port.in.ReceberFeedbackUseCase;
import br.com.fiap.feedback.api.infrastructure.adapter.in.lambda.dto.FeedbackRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Map;

@Named("feedback-handler")
@ApplicationScoped
public class FeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    ReceberFeedbackUseCase receberFeedbackUseCase;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            FeedbackRequest request = objectMapper.readValue(event.getBody(), FeedbackRequest.class);

            if (request.nota() == null) {
                return response(400, "{\"erro\": \"Campo 'nota' é obrigatório\"}");
            }

            Feedback feedback = receberFeedbackUseCase.receber(request.descricao(), request.nota());

            String body = objectMapper.writeValueAsString(Map.of(
                "id", feedback.getId(),
                "urgencia", feedback.getUrgencia().name(),
                "mensagem", "Feedback recebido com sucesso"
            ));

            Log.infof("Feedback recebido: id=%s urgencia=%s nota=%d", feedback.getId(), feedback.getUrgencia(), feedback.getNota());
            return response(201, body);

        } catch (IllegalArgumentException e) {
            Log.warnf("Requisição inválida: %s", e.getMessage());
            return response(400, "{\"erro\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            Log.errorf(e, "Erro ao processar feedback");
            return response(500, "{\"erro\": \"Erro interno ao processar feedback\"}");
        }
    }

    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(statusCode)
            .withHeaders(Map.of("Content-Type", "application/json"))
            .withBody(body)
            .withIsBase64Encoded(false);
    }
}
