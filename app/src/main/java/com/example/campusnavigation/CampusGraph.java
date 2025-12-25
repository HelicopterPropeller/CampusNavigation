package com.example.campusnavigation;

import android.content.Context;
import android.util.Pair;

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
        nodeList.removeIf(node -> node.id == id);
        edgeList.removeIf(edge -> edge.fromId == id || edge.toId == id);
    }

    public void addEdge(Edge edge) {
        if (adj.containsKey(edge.fromId)) {
            adj.get(edge.fromId).add(edge);
        }
        edgeList.add(edge);
    }

    public void removeEdge(Edge edge) {
        if (edge == null) return;
        if (adj.containsKey(edge.fromId)) {
            adj.get(edge.fromId).removeIf(e -> e.toId == edge.toId && e.waypoints.equals(edge.waypoints));
        }
        edgeList.remove(edge);
    }

    public Pair<Double, List<Edge>> getShortestPathWithEdges(int fromId, int toId) {
        if (fromId == toId) {
            return new Pair<>(0.0, new ArrayList<>());
        }

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Edge> prevEdge = new HashMap<>();

        for (Integer nodeId : nodes.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        
        if (!distances.containsKey(fromId)) return new Pair<>(-1.0, new ArrayList<>());

        distances.put(fromId, 0.0);

        PriorityQueue<NodeDist> pq =
                new PriorityQueue<>(Comparator.comparingDouble(a -> a.dist));
        pq.add(new NodeDist(fromId, 0.0));

        while (!pq.isEmpty()) {
            NodeDist current = pq.poll();
            int currentId = current.nodeId;

            if (current.dist > distances.get(currentId)) {
                continue;
            }

            List<Edge> neighbors = adj.get(currentId);
            if (neighbors == null) {
                continue;
            }

            for (Edge edge : neighbors) {
                int neighborId = edge.toId;
                double newDist = distances.get(currentId) + edge.distance;

                if (newDist < distances.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    distances.put(neighborId, newDist);
                    prevEdge.put(neighborId, edge);
                    pq.add(new NodeDist(neighborId, newDist));
                }
            }
        }

        if (!distances.containsKey(toId) || distances.get(toId) == Double.MAX_VALUE) {
            return new Pair<>(-1.0, new ArrayList<>());
        }

        List<Edge> path = new ArrayList<>();
        int cur = toId;

        while (cur != fromId) {
            Edge edge = prevEdge.get(cur);
            if (edge == null) {
                break;
            }
            path.add(edge);
            cur = edge.fromId;
        }

        reverseList(path);

        return new Pair<>(distances.get(toId), path);
    }

    private void reverseList(List<Edge> list) {
        int i = 0;
        int j = list.size() - 1;
        while (i < j) {
            Edge tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
            i++;
            j--;
        }
    }

    public void rebuildGraph() {
        nodes.clear();
        adj.clear();

        for (Node node : nodeList) {
            nodes.put(node.id, node);
            adj.put(node.id, new ArrayList<>());
        }

        for (Edge edge : edgeList) {
            if (adj.containsKey(edge.fromId)) {
                adj.get(edge.fromId).add(edge);
            }
        }
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
