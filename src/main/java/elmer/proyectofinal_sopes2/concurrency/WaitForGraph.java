package elmer.proyectofinal_sopes2.concurrency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Grafo RAG simplificado que detecta ciclos entre operaciones. */
public final class WaitForGraph {

    private final Map<String, Set<String>> assignments = new HashMap<>();
    private final Map<String, Set<String>> requests = new HashMap<>();

    /** Registra que una operacion posee una instancia o tipo de recurso. */
    public synchronized void addAssignment(String resource, String operation) {
        assignments.computeIfAbsent(resource, ignored -> new HashSet<>()).add(operation);
    }

    /** Registra que una operacion espera una instancia o tipo de recurso. */
    public synchronized void addRequest(String operation, String resource) {
        requests.computeIfAbsent(operation, ignored -> new HashSet<>()).add(resource);
    }

    public synchronized void removeAssignment(String resource, String operation) {
        remove(assignments, resource, operation);
    }

    public synchronized void removeRequest(String operation, String resource) {
        remove(requests, operation, resource);
    }

    /** Retorna un ciclo de operaciones o una lista vacia si el grafo esta sano. */
    public synchronized List<String> findCycle() {
        Map<String, Set<String>> processGraph = new HashMap<>();
        for (Map.Entry<String, Set<String>> request : requests.entrySet()) {
            for (String resource : request.getValue()) {
                for (String owner : assignments.getOrDefault(resource, Set.of())) {
                    addDependency(processGraph, request.getKey(), owner);
                }
            }
        }

        for (String start : processGraph.keySet()) {
            List<String> path = new ArrayList<>();
            if (findCycle(start, start, processGraph, new HashSet<>(), path)) {
                return List.copyOf(path);
            }
        }
        return List.of();
    }

    public synchronized Map<String, Set<String>> snapshotAssignments() {
        return copyGraph(assignments);
    }

    public synchronized Map<String, Set<String>> snapshotRequests() {
        return copyGraph(requests);
    }

    private boolean findCycle(String start, String current, Map<String, Set<String>> graph,
                              Set<String> visited, List<String> path) {
        visited.add(current);
        path.add(current);
        for (String next : graph.getOrDefault(current, Set.of())) {
            if (next.equals(start)) {
                path.add(start);
                return true;
            }
            if (!visited.contains(next) && findCycle(start, next, graph, visited, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    private static void remove(Map<String, Set<String>> graph, String source, String target) {
        Set<String> values = graph.get(source);
        if (values != null) {
            values.remove(target);
            if (values.isEmpty()) {
                graph.remove(source);
            }
        }
    }

    private static void addDependency(Map<String, Set<String>> graph, String operation, String owner) {
        if (!operation.equals(owner)) {
            graph.computeIfAbsent(operation, ignored -> new HashSet<>()).add(owner);
        }
    }

    private static Map<String, Set<String>> copyGraph(Map<String, Set<String>> graph) {
        Map<String, Set<String>> copy = new HashMap<>();
        graph.forEach((key, values) -> copy.put(key, Set.copyOf(values)));
        return Map.copyOf(copy);
    }
}