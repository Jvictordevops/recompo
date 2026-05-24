package com.vic.recompo.domain.model

enum class Sexo { HOMBRE, MUJER }

enum class SlotComida { DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA }

enum class GrupoMuscular {
    PECHO, ESPALDA, HOMBRO, BICEPS, TRICEPS,
    ANTEBRAZO, CUADRICEPS, ISQUIOTIBIALES, GLUTEO,
    PANTORRILLA, CORE, TRAPECIO
}

enum class PatronMovimiento {
    EMPUJE_HORIZONTAL, EMPUJE_VERTICAL,
    TIRON_HORIZONTAL, TIRON_VERTICAL,
    BISAGRA, SENTADILLA, LUNGES, AISLAMIENTO, CORE, CARDIO
}

enum class TipoSesion { A, B, C }

enum class EstadoSesion { PLANIFICADA, EN_CURSO, COMPLETADA, OMITIDA }

enum class OrigenSesion { SEED, IA, MANUAL }

enum class TipoConversacion { NUTRICIONAL, ENTRENO }

enum class RolMensaje { USER, ASSISTANT }
