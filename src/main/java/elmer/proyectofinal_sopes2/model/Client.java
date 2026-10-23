package elmer.proyectofinal_sopes2.model;

import java.util.Objects;

/**
 * cuenta de cliente registrada.
 * 
 * cliente es nuevo, debe poder crear una cuenta
 * 
 */
public class Client {

    private final String id;
    private final String nombre;
    private final ServiceLevel nivelServicioDefault;

    public Client(String id, String nombre, ServiceLevel nivelServicioDefault) {
        this.id = Objects.requireNonNull(id, "El ID del cliente no puede ser nulo");
        this.nombre = Objects.requireNonNull(nombre, "El nombre del cliente no puede ser nulo");
        this.nivelServicioDefault = Objects.requireNonNull(nivelServicioDefault, "El nivel de servicio no puede ser nulo");
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public ServiceLevel getNivelServicioDefault() {
        return nivelServicioDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ") - " + nivelServicioDefault.getDisplayName();
    }
}
