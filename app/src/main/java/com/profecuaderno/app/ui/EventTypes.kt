package com.profecuaderno.app.ui

data class EventTypeOption(val code: String, val label: String)

val teacherEventTypes = listOf(
    EventTypeOption("TEMA_CLASE", "Tema / clase"),
    EventTypeOption("PRACTICA", "Práctica"),
    EventTypeOption("LABORATORIO", "Laboratorio"),
    EventTypeOption("EXAMEN", "Examen"),
    EventTypeOption("EVALUACION_PARCIAL", "Evaluación parcial"),
    EventTypeOption("EXPOSICION", "Exposición"),
    EventTypeOption("INVESTIGACION_MODULAR", "Investigación modular"),
    EventTypeOption("MAQUETA", "Entrega de maqueta"),
    EventTypeOption("ENTREGA", "Entrega"),
    EventTypeOption("DOCENTE_INVITADO", "Docente / especialista invitado"),
    EventTypeOption("VISITA", "Visita"),
    EventTypeOption("SALIDA", "Salida / actividad externa"),
    EventTypeOption("IMPORTANTE", "Fecha importante"),
    EventTypeOption("OTRO", "Otro")
)

fun eventTypeLabel(code: String): String =
    teacherEventTypes.firstOrNull { it.code == code }?.label ?: code.replace('_', ' ').lowercase()
