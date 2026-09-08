package co.edu.uniminuto.hyeda.notifications.domain;

public enum NotificationStatus {

    EN_COLA,
    ENVIADA,
    /** Agoto los reintentos; queda visible para reenviar a mano. */
    FALLIDA,
    /** Se decidio no enviarla (cliente sin telefono, canal apagado). */
    DESCARTADA
}
