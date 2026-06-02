package br.com.fiap.feedback.notification.application.service;

import br.com.fiap.feedback.notification.domain.model.Notificacao;
import br.com.fiap.feedback.notification.domain.port.in.EnviarNotificacaoUseCase;
import br.com.fiap.feedback.notification.domain.port.out.EnviarEmailPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EnviarNotificacaoService implements EnviarNotificacaoUseCase {

    @Inject
    EnviarEmailPort enviarEmailPort;

    @ConfigProperty(name = "app.admin.email")
    String adminEmail;

    @Override
    public void enviar(Notificacao notificacao) {
        String assunto = String.format("[URGENTE] Feedback crítico recebido - Nota: %d", notificacao.getNota());
        String corpo = montarCorpoEmail(notificacao);

        enviarEmailPort.enviar(adminEmail, assunto, corpo);
        Log.infof("Notificação urgente enviada: feedbackId=%s para=%s", notificacao.getFeedbackId(), adminEmail);
    }

    private String montarCorpoEmail(Notificacao notificacao) {
        return String.format("""
            <html>
            <body>
                <h2 style="color: red;">⚠️ Alerta: Feedback Crítico Recebido</h2>
                <table>
                    <tr><td><strong>Descrição:</strong></td><td>%s</td></tr>
                    <tr><td><strong>Urgência:</strong></td><td style="color: red;">%s</td></tr>
                    <tr><td><strong>Nota:</strong></td><td>%d / 10</td></tr>
                    <tr><td><strong>Data de envio:</strong></td><td>%s</td></tr>
                    <tr><td><strong>ID do Feedback:</strong></td><td>%s</td></tr>
                </table>
                <p>Este feedback requer atenção imediata.</p>
            </body>
            </html>
            """,
            notificacao.getDescricao(),
            notificacao.getUrgencia(),
            notificacao.getNota(),
            notificacao.getDataEnvio(),
            notificacao.getFeedbackId()
        );
    }
}
