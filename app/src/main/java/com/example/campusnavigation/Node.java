package com.example.campusnavigation;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    public final int id;

    public final LatLng position;

    public final String name;

    public Node(LatLng position, String name) {
        this.id = ID_GENERATOR.incrementAndGet();;
        this.position = position;
        this.name = name;
    }
}