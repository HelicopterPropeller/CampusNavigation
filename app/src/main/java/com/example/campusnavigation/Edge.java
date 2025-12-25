package com.example.campusnavigation;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.List;

public class Edge {
    public final int fromId;
    public final int toId;
    public double distance;
    public List<LatLng> waypoints;

    public Edge(int fromId, int toId, double distance, List<LatLng> waypoints) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.waypoints = waypoints;
    }
}
