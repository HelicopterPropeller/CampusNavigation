package com.example.campusnavigation;

public class Edge {
    public final int fromId;
    public final int toId;
    public double distance;

    public Edge(int fromId, int toId, double distance) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
    }
}
