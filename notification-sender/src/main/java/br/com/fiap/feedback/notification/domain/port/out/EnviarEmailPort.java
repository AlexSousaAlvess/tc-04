package br.com.fiap.feedback.notification.domain.port.out;

public interface EnviarEmailPort {

    void enviar(String destinatario, String assunto, String corpo);
}
