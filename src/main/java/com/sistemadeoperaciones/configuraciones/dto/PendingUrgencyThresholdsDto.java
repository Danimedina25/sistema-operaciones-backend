package com.sistemadeoperaciones.configuraciones.dto;

/** Umbrales del semáforo de pendientes; los consultan todos los roles. */
public record PendingUrgencyThresholdsDto(int horasPendienteAtencion, int horasPendienteUrgente) {
}
