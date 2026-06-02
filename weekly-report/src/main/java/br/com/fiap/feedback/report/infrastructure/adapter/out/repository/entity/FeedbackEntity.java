package br.com.fiap.feedback.report.infrastructure.adapter.out.repository.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class FeedbackEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "feedback_id")
    public String feedbackId;

    @Column(columnDefinition = "TEXT")
    public String descricao;

    public Integer nota;

    @Column(length = 10)
    public String urgencia;

    @Column(name = "criado_em")
    public LocalDateTime criadoEm;

    @Column(name = "processado_em")
    public LocalDateTime processadoEm;
}
