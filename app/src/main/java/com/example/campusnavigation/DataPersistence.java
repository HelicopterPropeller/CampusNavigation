package com.example.campusnavigation;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataPersistence {

    private static final String PREFS_NAME = "CampusDataPrefs";
    private static final String KEY_NODES = "nodes";
    private static final String KEY_EDGES = "edges";

    public static void saveData(Context context, List<Node> nodes, List<Edge> edges) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();

        String nodesJson = gson.toJson(nodes);
        String edgesJson = gson.toJson(edges);

        editor.putString(KEY_NODES, nodesJson);
        editor.putString(KEY_EDGES, edgesJson);

        editor.commit();
    }

    public static List<Node> loadNodes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String nodesJson = prefs.getString(KEY_NODES, null);

        if (nodesJson == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<List<Node>>(){}.getType();
        List<Node> list = gson.fromJson(nodesJson, type);

        return list != null ? list : new ArrayList<>();
    }

    public static List<Edge> loadEdges(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String edgesJson = prefs.getString(KEY_EDGES, null);

        if (edgesJson == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<List<Edge>>() {}.getType();
        List<Edge> list = gson.fromJson(edgesJson, type);

        return list != null ? list : new ArrayList<>();
    }
}