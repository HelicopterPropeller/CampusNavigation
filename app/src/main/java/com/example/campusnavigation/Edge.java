package com.example.campusnavigation;

public class Edge {
    public final int fromId;
    public final int toId;
    public double distance;      // 米
    public double beautyScore;   // 景色评分
    public double shadeScore;    // 绿荫评分

    public Edge(int fromId, int toId, double distance,
                double beautyScore, double shadeScore) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.beautyScore = beautyScore;
        this.shadeScore = shadeScore;
    }
}
