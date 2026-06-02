package br.com.fiap.feedback.processor.infrastructure.adapter.out.repository.entity;

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

    @Column(name = "feedback_id", nullable = false, unique = true)
    public String feedbackId;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String descricao;

    @Column(nullable = false)
    public Integer nota;

    @Column(nullable = false, length = 10)
    public String urgencia;

    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;

    @Column(name = "processado_em", nullable = false)
    public LocalDateTime processadoEm;
}
