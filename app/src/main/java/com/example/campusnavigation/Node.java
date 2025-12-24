package com.example.campusnavigation;

import com.tencent.tencentmap.mapsdk.maps.model.LatLng;

import java.util.concurrent.atomic.AtomicInteger;

public class Node {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    public final int id;

    public final LatLng position;

    public String name;
    public String description;

    public Node(LatLng position, String name, String description) {
        this.id = ID_GENERATOR.incrementAndGet();;
        this.position = position;
        this.name = name;
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}