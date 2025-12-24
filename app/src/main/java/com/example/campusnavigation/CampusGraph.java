package com.example.campusnavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampusGraph {
    public Map<Integer, Node> nodes = new HashMap<>();
    public Map<Integer, List<Edge>> adj = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.id, node);
        adj.putIfAbsent(node.id, new ArrayList<>());
    }

    public void removeNode(int id) {
        nodes.remove(id);
        adj.remove(id);
        for (List<Edge> edges : adj.values()) {
            edges.removeIf(edge -> edge.toId == id);
        }
    }

    public void addEdge(Edge edge) {
        if (adj.containsKey(edge.fromId)) {
            adj.get(edge.fromId).add(edge);
        }
    }

    public void removeEdge(int fromId, int toId) {
        if (adj.containsKey(fromId)) {
            adj.get(fromId).removeIf(edge -> edge.toId == toId);
        }
    }
}
