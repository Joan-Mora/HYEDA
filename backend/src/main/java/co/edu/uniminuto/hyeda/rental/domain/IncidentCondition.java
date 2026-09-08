package co.edu.uniminuto.hyeda.rental.domain;

public enum IncidentCondition {

    /** Vuelve averiado; se cobra reparacion o reposicion. */
    DANIO,
    /** No vuelve; se cobra el valor de reposicion y se da de baja del inventario. */
    FALTANTE,
    /** El cliente reclama por el estado del equipo recibido. */
    INCONFORMIDAD,
    /** Desgaste normal de uso; no se cobra. */
    DESGASTE;

    /** Solo dano y faltante sacan la unidad del inventario. */
    public boolean removesFromInventory() {
        return this == DANIO || this == FALTANTE;
    }
}
