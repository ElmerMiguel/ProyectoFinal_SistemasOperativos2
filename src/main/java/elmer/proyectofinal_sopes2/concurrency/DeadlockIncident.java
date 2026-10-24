package elmer.proyectofinal_sopes2.concurrency;

import elmer.proyectofinal_sopes2.model.Order;
import elmer.proyectofinal_sopes2.model.ResourceType;

import java.util.Objects;

/** Describe dos operaciones y los recursos que forman una espera circular. */
public record DeadlockIncident(Order operationA, ResourceType heldByA, ResourceType requestedByA,
                               Order operationB, ResourceType heldByB, ResourceType requestedByB,
                               long detectedAt, String explanation) {

    public DeadlockIncident {
        Objects.requireNonNull(operationA, "La operacion A es obligatoria");
        Objects.requireNonNull(operationB, "La operacion B es obligatoria");
        Objects.requireNonNull(heldByA, "El recurso retenido por A es obligatorio");
        Objects.requireNonNull(requestedByA, "El recurso solicitado por A es obligatorio");
        Objects.requireNonNull(heldByB, "El recurso retenido por B es obligatorio");
        Objects.requireNonNull(requestedByB, "El recurso solicitado por B es obligatorio");
        Objects.requireNonNull(explanation, "La explicacion es obligatoria");
    }
}