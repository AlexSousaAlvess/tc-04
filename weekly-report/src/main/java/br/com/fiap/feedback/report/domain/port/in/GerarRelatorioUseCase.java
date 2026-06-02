package br.com.fiap.feedback.report.domain.port.in;

import br.com.fiap.feedback.report.domain.model.RelatorioSemanal;

import java.time.LocalDate;

public interface GerarRelatorioUseCase {

    RelatorioSemanal gerar(LocalDate dataInicio, LocalDate dataFim);
}
