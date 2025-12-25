package com.example.campusnavigation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private MapView map;
    private TencentMap tencentMap;

    private boolean correcting = false;
    private int padding;
    private LatLngBounds bounds;
    private LatLng boundsCenter;

    private final Map<Marker, Node> markerToNode = new LinkedHashMap<>();
    private final Map<Node, Marker> nodeToMarker = new LinkedHashMap<>();
    private final Map<Polyline, Pair<Edge, Edge>> polylineToEdge = new LinkedHashMap<>();
    private final Map<Marker, List<Polyline>> markerToPolyline = new LinkedHashMap<>();

    private MaterialButtonToggleGroup toggleGroup;
    private View bottomSheet;
    private TextView button_1;
    private TextView button_2;
    private Button exportButton;

    private final CampusGraph graph = new CampusGraph();
    private final List<Marker> routePoints = new ArrayList<>();
    private Marker edgeStart = null;
    private Marker viewStart = null;
    private final List<Polyline> guidePolylines = new ArrayList<>();

    /**
     * 更新按钮显隐性且带透明度动画
     */
    private void updateButtonVisibility(View button, boolean show) {
        if (show) {
            if (button.getVisibility() == View.VISIBLE && button.getAlpha() == 1.0f) return;
            button.setVisibility(View.VISIBLE);
            button.animate().alpha(1.0f).setDuration(200).start();
        } else {
            if (button.getVisibility() == View.GONE || (button.getVisibility() == View.VISIBLE && button.getAlpha() == 0.0f)) return;
            button.animate().alpha(0.0f).setDuration(200).withEndAction(() -> button.setVisibility(View.GONE)).start();
        }
    }

    /**
     * 编辑节点模式的地图点击监听器
     */
    private final TencentMap.OnMapClickListener nodeMapCL = latLng -> {
        EditDialogFragment dialog = new EditDialogFragment();

        dialog.setOnConfirmListener((name, description) -> {
            if (name.isEmpty()) {
                Toast.makeText(this, "地点名称不应为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Node node = new Node(latLng, name, description);
            MarkerOptions options = new MarkerOptions(latLng).infoWindowEnable(false).title(name).snippet(description);
            Marker marker = tencentMap.addMarker(options);

            marker.setInfoWindowEnable(true);
            marker.setClickable(true);

            markerToNode.put(marker, node);
            nodeToMarker.put(node, marker);
            markerToPolyline.put(marker, new ArrayList<>());

            graph.addNode(node);
            graph.saveData(this);
        });

        dialog.show(getSupportFragmentManager(), "EditDialog");
    };

    /**
     * 编辑节点模式的Marker点击监听器
     */
    private final TencentMap.OnMarkerClickListener nodeMarkerCL = marker -> {
        Node node = markerToNode.get(marker);
        if (node == null) return false;

        if (marker.isInfoWindowShown()) {

            marker.hideInfoWindow();

            updateButtonVisibility(button_1, false);
            updateButtonVisibility(button_2, false);
        } else {

            marker.showInfoWindow();

            button_1.setText("更改");
            button_2.setText("删除");

            updateButtonVisibility(button_1, true);
            updateButtonVisibility(button_2, true);

            button_1.setOnClickListener(v -> {
                EditDialogFragment dialog = new EditDialogFragment();

                dialog.setOnConfirmListener((name, description) -> {
                    if (name.isEmpty()) {
                        Toast.makeText(this, "地点名称不应为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    node.setName(name);
                    node.setDescription(description);
                    marker.getOptions().title(name);
                    marker.getOptions().snippet(description);
                    marker.showInfoWindow();
                    graph.saveData(this);
                });

                dialog.show(getSupportFragmentManager(), "EditDialog");
            });

            button_2.setOnClickListener(v -> {
                Node temp = markerToNode.get(marker);
                markerToNode.remove(marker);
                List<Polyline> polylines = markerToPolyline.get(marker);

                for (Polyline polyline : polylines) polyline.remove();
                markerToPolyline.remove(marker);

                graph.removeNode(temp.id);
                marker.remove();

                updateButtonVisibility(button_1, false);
                updateButtonVisibility(button_2, false);

                graph.saveData(this);
                Toast.makeText(this, "地点标记已删除", Toast.LENGTH_SHORT).show();
            });
        }
        return true;
    };

    /**
     * 浏览模式的地图点击监听器
     */
    private final TencentMap.OnMapClickListener viewMapCL = latLng -> {};

    /**
     * 浏览模式的Marker点击监听器
     */
    private final TencentMap.OnMarkerClickListener viewMarkerCL = marker -> {
        Node node = markerToNode.get(marker);
        if (node == null) return false;

        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
            updateButtonVisibility(button_1, false);
            updateButtonVisibility(button_2, false);
        } else {
            marker.showInfoWindow();
            button_1.setText("从这来");
            button_2.setText("到这去");
            updateButtonVisibility(button_1, true);
            updateButtonVisibility(button_2, true);
        }

        button_1.setOnClickListener(v -> {
            viewStart = marker;
            Toast.makeText(this, "起点已设为: " + node.name, Toast.LENGTH_SHORT).show();
            updateButtonVisibility(button_1, false);
            updateButtonVisibility(button_2, false);
            marker.hideInfoWindow();
        });

        button_2.setOnClickListener(v -> {
            if (viewStart == null) {
                Toast.makeText(this, "请首先选择起点", Toast.LENGTH_SHORT).show();
                return;
            }
            if (viewStart.equals(marker)) {
                Toast.makeText(this, "终点和起点不能相同", Toast.LENGTH_SHORT).show();
                return;
            }

            Node from = markerToNode.get(viewStart);
            Node to = node;
            if (from == null || to == null) return;

            Pair<Double, List<Edge>> pair = graph.getShortestPathWithEdges(from.id, to.id);
            double distance = pair.first;

            if (distance < 0) {
                Toast.makeText(this, "无法到达该地点", Toast.LENGTH_SHORT).show();
            } else {
                for (Polyline p : guidePolylines) p.remove();
                guidePolylines.clear();

                for (Edge edge : pair.second) {
                    Polyline polyline = tencentMap.addPolyline(new PolylineOptions().addAll(edge.waypoints).color(0xFF0000FF).width(10));
                    guidePolylines.add(polyline);
                }
                Toast.makeText(this, "从 " + from.name + " 到 " + to.name + " 距离: " + (int) distance + "米", Toast.LENGTH_SHORT).show();
            }

            updateButtonVisibility(button_1, false);
            updateButtonVisibility(button_2, false);
            marker.hideInfoWindow();
            viewStart = null;
        });
        return true;
    };

    /**
     * 编辑道路模式的地图点击监听器
     */
    private final TencentMap.OnMapClickListener edgeMapCL = latLng -> {
        MarkerOptions mp = new MarkerOptions(latLng);
        Marker marker = tencentMap.addMarker(mp);
        routePoints.add(marker);
    };

    /**
     * 编辑道路模式的Marker点击监听器
     */
    private final TencentMap.OnMarkerClickListener edgeMarkerCL = marker -> {
        if (edgeStart == null) {
            edgeStart = marker;

            for (Marker m : routePoints) m.remove();
            routePoints.clear();
        } else {
            if (edgeStart == marker) {
                Toast.makeText(this, "不能在同一地点间建立道路", Toast.LENGTH_SHORT).show();
                edgeStart = null;
                for (Marker m : routePoints) m.remove();
                routePoints.clear();
                return true;
            }

            Pair<Double, Polyline> pair = drawRoute(edgeStart, marker);
            double distance = pair.first;
            Polyline polyline = pair.second;

            markerToPolyline.get(edgeStart).add(polyline);
            markerToPolyline.get(marker).add(polyline);

            Node from = markerToNode.get(edgeStart);
            Node to = markerToNode.get(marker);

            List<LatLng> waypoints = routePoints.stream()
                    .map(Marker::getPosition)
                    .collect(java.util.stream.Collectors.toList());

            Edge edge1 = new Edge(from.id, to.id, distance, waypoints);

            List<LatLng> reverseWaypoints = new ArrayList<>(waypoints);
            java.util.Collections.reverse(reverseWaypoints);
            Edge edge2 = new Edge(to.id, from.id, distance, reverseWaypoints);

            for (int i = 1; i < routePoints.size() - 1; ++i) {
                routePoints.get(i).remove();
            }

            graph.addEdge(edge1);
            graph.addEdge(edge2);

            polylineToEdge.put(polyline, new Pair<>(edge1, edge2));

            routePoints.clear();
            edgeStart = null;
            graph.saveData(this);
        }
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        padding = (int) (getResources().getDisplayMetrics().density * 16);
        map = findViewById(R.id.content);
        TencentMapInitializer.setAgreePrivacy(true);

        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(new LatLng(34.15711406, 108.90797981));
        latLngs.add(new LatLng(34.14858452597058, 108.8977919290345));

        bounds = LatLngBounds.builder().include(latLngs).build();
        boundsCenter = bounds.getCenter();

        tencentMap = map.getMap();
        tencentMap.getUiSettings().setRotateGesturesEnabled(false);
        tencentMap.getUiSettings().setTiltGesturesEnabled(false);
        tencentMap.setMapType(TencentMap.MAP_TYPE_NONE);

        // 地图加载完成回调
        tencentMap.setOnMapLoadedCallback(() -> {
            tencentMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            map.post(() -> {
                float fixedZoom = tencentMap.getCameraPosition().zoom;
                tencentMap.setMinZoomLevel((int) fixedZoom);
            });
        });

        // 相机变化监听
        tencentMap.setOnCameraChangeListener(new TencentMap.OnCameraChangeListener() {
            @Override public void onCameraChange(CameraPosition cameraPosition) {}
            @Override public void onCameraChangeFinished(CameraPosition cameraPosition) {
                if (correcting) return;
                LatLng screenCenter = cameraPosition.target;
                double latSpan = bounds.getNorthEast().latitude - bounds.getSouthWest().latitude;
                double lngSpan = bounds.getNorthEast().longitude - bounds.getSouthWest().longitude;

                if (Math.abs(screenCenter.latitude - boundsCenter.latitude) > latSpan * 0.5 
                        || Math.abs(screenCenter.longitude - boundsCenter.longitude) > lngSpan * 0.5) {
                    reformat();
                }
            }
        });

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheet.post(() -> {
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setExpandedOffset(0);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        toggleGroup = findViewById(R.id.mode_toggle_group);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            reset();

            if (checkedId == R.id.view_mode) {

                tencentMap.setOnMapClickListener(viewMapCL);
                tencentMap.setOnMarkerClickListener(viewMarkerCL);

                tencentMap.setOnPolylineClickListener(null);
            } else if (checkedId == R.id.node_mode) {

                tencentMap.setOnMapClickListener(nodeMapCL);
                tencentMap.setOnMarkerClickListener(nodeMarkerCL);

                tencentMap.setOnPolylineClickListener(null);
            } else if (checkedId == R.id.edge_mode) {

                tencentMap.setOnMapClickListener(edgeMapCL);
                tencentMap.setOnMarkerClickListener(edgeMarkerCL);

                tencentMap.setOnPolylineClickListener((polyline, latLng) -> {
                    button_2.setText("删除");
                    if (button_2.getVisibility() == View.VISIBLE && button_2.getAlpha() == 1.0f) {
                        updateButtonVisibility(button_2, false);
                    } else {
                        updateButtonVisibility(button_2, true);
                        button_2.setOnClickListener(v -> {
                            Pair<Edge, Edge> pair = polylineToEdge.get(polyline);
                            if (pair != null) {
                                polyline.remove();
                                graph.removeEdge(pair.first);
                                graph.removeEdge(pair.second);
                                updateButtonVisibility(button_2, false);
                                graph.saveData(this);
                                Toast.makeText(this, "道路标记已删除", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        exportButton = findViewById(R.id.export_button);
        exportButton.setOnClickListener(v -> export());

        button_1 = findViewById(R.id.button_1);
        button_2 = findViewById(R.id.button_2);

        button_1.setVisibility(View.GONE);
        button_1.setAlpha(0.0f);
        button_2.setVisibility(View.GONE);
        button_2.setAlpha(0.0f);

        toggleGroup.check(R.id.view_mode);
    }

    /**
     * 导出地图数据为 PDF
     */
    private void export() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 尺寸
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int y = 50;

        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("校园导航数据", 50, y, paint);
        y += 40;

        paint.setTextSize(14);
        canvas.drawText("节点列表:", 50, y, paint);
        y += 25;
        paint.setFakeBoldText(false);

        for (Node node : graph.nodeList) {
            if (y > 780) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            String info = String.format("ID: %d | 名称: %s | 坐标: (%.6f, %.6f)",
                    node.id, node.name, node.position.latitude, node.position.longitude);
            canvas.drawText(info, 70, y, paint);
            y += 20;
            canvas.drawText("   描述: " + node.description, 70, y, paint);
            y += 30;
        }

        y += 10;
        paint.setFakeBoldText(true);
        canvas.drawText("道路列表:", 50, y, paint);
        y += 25;
        paint.setFakeBoldText(false);

        for (Edge edge : graph.edgeList) {
            if (y > 780) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            Node from = graph.nodes.get(edge.fromId);
            Node to = graph.nodes.get(edge.toId);
            String fromName = from != null ? from.name : "未知节点";
            String toName = to != null ? to.name : "未知节点";

            String edgeInfo = String.format("连接: [%s] -> [%s] | 长度: %.1f米", fromName, toName, edge.distance);
            canvas.drawText(edgeInfo, 70, y, paint);
            y += 25;
        }

        document.finishPage(page);

        File file = new File(getExternalFilesDir(null), "CampusGraphReport.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }

    /**
     * 重置地图相机至默认范围
     */
    private void reformat() {
        correcting = true;
        tencentMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), new TencentMap.CancelableCallback() {
            @Override public void onFinish() { correcting = false; }
            @Override public void onCancel() { correcting = false; }
        });
    }

    /**
     * 重置UI状态和地图元素
     */
    private void reset() {
        updateButtonVisibility(button_1, false);
        updateButtonVisibility(button_2, false);

        for (Marker marker : markerToNode.keySet()) marker.hideInfoWindow();
        for (Marker marker : routePoints) marker.remove();
        routePoints.clear();

        for (Polyline p : guidePolylines) p.remove();
        guidePolylines.clear();
    }

    /**
     * 绘制两点间的道路
     */
    private Pair<Double, Polyline> drawRoute(Marker start, Marker end) {
        double totalDistance = 0;

        routePoints.add(0, start);
        routePoints.add(end);

        for (int i = 0; i < routePoints.size() - 1; i++) {
            totalDistance += calculateDistance(
                    routePoints.get(i).getPosition(),
                    routePoints.get(i + 1).getPosition());
        }

        List<LatLng> list = routePoints.stream()
                .map(Marker::getPosition)
                .collect(java.util.stream.Collectors.toList());

        Polyline polyline = tencentMap.addPolyline(new PolylineOptions()
                .addAll(list)
                .width(6));

        return new Pair<>(totalDistance, polyline);
    }

    /**
     * 计算球面两点间的距离
     */
    public double calculateDistance(LatLng startPoint, LatLng endPoint) {
        double R = 6371000;
        double lat1 = Math.toRadians(startPoint.latitude);
        double lon1 = Math.toRadians(startPoint.longitude);
        double lat2 = Math.toRadians(endPoint.latitude);
        double lon2 = Math.toRadians(endPoint.longitude);

        double d_lat = lat2 - lat1;
        double d_lon = lon2 - lon1;
        double a = Math.sin(d_lat / 2) * Math.sin(d_lat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(d_lon / 2) * Math.sin(d_lon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    protected void onStart() {
        super.onStart();
        map.onStart();

        tencentMap.clear();
        markerToNode.clear();
        nodeToMarker.clear();
        markerToPolyline.clear();
        polylineToEdge.clear();

        graph.nodeList = DataPersistence.loadNodes(this);
        graph.edgeList = DataPersistence.loadEdges(this);

        int maxId = 0;
        for (Node node : graph.nodeList) if (node.id > maxId) maxId = node.id;
        Node.setGeneratorValue(maxId);

        graph.rebuildGraph();

        for (Node node : graph.nodeList) {
            Marker marker = tencentMap.addMarker(new MarkerOptions(node.position).title(node.name).snippet(node.description));
            markerToNode.put(marker, node);
            nodeToMarker.put(node, marker);
            markerToPolyline.put(marker, new ArrayList<>());
        }

        List<String> drawnEdgeKeys = new ArrayList<>();

        for (Edge edge : graph.edgeList) {
            if (edge.waypoints == null || edge.waypoints.size() < 2) continue;

            String edgeKey = edge.fromId < edge.toId ?
                    edge.fromId + "_" + edge.toId : edge.toId + "_" + edge.fromId;

            if (drawnEdgeKeys.contains(edgeKey)) continue;

            Polyline polyline = tencentMap.addPolyline(new PolylineOptions()
                    .addAll(edge.waypoints)
                    .width(6));

            drawnEdgeKeys.add(edgeKey);

            Node fromNode = graph.nodes.get(edge.fromId);
            Node toNode = graph.nodes.get(edge.toId);
            Marker fromMarker = nodeToMarker.get(fromNode);
            Marker toMarker = nodeToMarker.get(toNode);

            if (fromMarker != null) markerToPolyline.get(fromMarker).add(polyline);
            if (toMarker != null) markerToPolyline.get(toMarker).add(polyline);

            Edge reverseEdge = graph.edgeList.stream()
                    .filter(e -> e.fromId == edge.toId && e.toId == edge.fromId)
                    .findFirst().orElse(null);

            polylineToEdge.put(polyline, new Pair<>(edge, reverseEdge));
        }
    }

    @Override protected void onStop() {
        super.onStop();
        map.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
    }
}