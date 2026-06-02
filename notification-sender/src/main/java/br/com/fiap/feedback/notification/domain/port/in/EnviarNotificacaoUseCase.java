package br.com.fiap.feedback.notification.domain.port.in;

import br.com.fiap.feedback.notification.domain.model.Notificacao;

public interface EnviarNotificacaoUseCase {

    void enviar(Notificacao notificacao);
}
