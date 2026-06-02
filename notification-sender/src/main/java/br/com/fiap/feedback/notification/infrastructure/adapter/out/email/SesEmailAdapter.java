package br.com.fiap.feedback.notification.infrastructure.adapter.out.email;

import br.com.fiap.feedback.notification.domain.port.out.EnviarEmailPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@ApplicationScoped
public class SesEmailAdapter implements EnviarEmailPort {

    @Inject
    SesClient sesClient;

    @ConfigProperty(name = "app.ses.sender-email")
    String senderEmail;

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder()
                    .toAddresses(destinatario)
                    .build())
                .message(Message.builder()
                    .subject(Content.builder()
                        .data(assunto)
                        .charset("UTF-8")
                        .build())
                    .body(Body.builder()
                        .html(Content.builder()
                            .data(corpo)
                            .charset("UTF-8")
                            .build())
                        .build())
                    .build())
                .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            Log.infof("E-mail enviado via SES: messageId=%s para=%s", response.messageId(), destinatario);
        } catch (Exception e) {
            Log.errorf(e, "Erro ao enviar e-mail via SES para: %s", destinatario);
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }
}
