package br.com.fiap.feedback.report.domain.port.out;

import br.com.fiap.feedback.report.domain.model.RelatorioSemanal;

public interface EnviarRelatorioPort {

    void enviar(RelatorioSemanal relatorio);
}
