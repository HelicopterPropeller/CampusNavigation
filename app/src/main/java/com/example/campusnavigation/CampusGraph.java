package com.example.campusnavigation;

import android.content.Context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CampusGraph {
    public Map<Integer, Node> nodes = new HashMap<>();
    public Map<Integer, List<Edge>> adj = new HashMap<>();
    public List<Node> nodeList = new ArrayList<>();
    public List<Edge> edgeList = new ArrayList<>();

    public void addNode(Node node) {
        nodes.put(node.id, node);
        adj.putIfAbsent(node.id, new ArrayList<>());
        nodeList.add(node);
    }

    public void removeNode(int id) {
        nodes.remove(id);
        adj.remove(id);
        for (List<Edge> edges : adj.values()) {
            edges.removeIf(edge -> edge.toId == id);
        }
        nodeList.remove(nodes.get(id));
    }

    public void addEdge(Edge edge) {
        if (adj.containsKey(edge.fromId)) {
            adj.get(edge.fromId).add(edge);
        }
        if (adj.containsKey(edge.toId)) {
            Edge reverseEdge = new Edge(edge.toId, edge.fromId, edge.distance, edge.waypoints);
            adj.get(edge.toId).add(reverseEdge);
        }
        edgeList.add(edge);
    }

    public void removeEdge(Edge edge) {
        if (adj.containsKey(edge.fromId)) {
            adj.get(edge.fromId).removeIf(e -> e.toId == edge.toId);
        }
        if (adj.containsKey(edge.toId)) {
            adj.get(edge.toId).removeIf(e -> e.toId == edge.fromId);
        }
        /* edgeList删除道路逻辑 */
        edgeList.remove(edge);
    }

    public double getShortestPath(int fromId, int toId) {
        if (fromId == toId) return 0;

        Map<Integer, Double> distances = new HashMap<>();
        for (Integer nodeId : nodes.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        distances.put(fromId, 0.0);

        PriorityQueue<NodeDist> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a.dist));
        pq.add(new NodeDist(fromId, 0.0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            int currentNodeId = current.nodeId;
            double currentDist = current.dist;

            if (currentDist > distances.get(currentNodeId)) continue;

            for (Edge edge : adj.get(currentNodeId)) {
                int neighborId = edge.toId;
                double newDist = currentDist + edge.distance;

                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    pq.add(new NodeDist(neighborId, newDist));
                }
            }
        }

        return distances.get(toId) == Double.MAX_VALUE ? -1 : distances.get(toId);
    }

    public void saveData(Context context) {
        DataPersistence.saveData(context, nodeList, edgeList);
    }

    private static class NodeDist {
        int nodeId;
        double dist;

        NodeDist(int nodeId, double dist) {
            this.nodeId = nodeId;
            this.dist = dist;
        }
    }
}
