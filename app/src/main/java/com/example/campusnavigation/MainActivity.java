package com.example.campusnavigation;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
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
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptor;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;

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
    private Map<Marker, Node> markerToNode = new LinkedHashMap<>();
    private Map<Marker, List<Polyline>> markerToPolyline = new LinkedHashMap<>();
    private boolean isShow = false;
    private MaterialButtonToggleGroup toggleGroup;
    private View bottomSheet;
    private TextView button_1;
    private TextView button_2;
    private final CampusGraph graph = new CampusGraph();
    private List<Marker> routePoints = new ArrayList<>();
    private Marker edgeStart = null;
    private Marker viewStart = null;

    private TencentMap.OnMapClickListener nodeMapCL = latLng -> {
        /* 点击设立地点 弹出弹窗以编辑地点信息 */
        EditDialogFragment dialog = new EditDialogFragment();

        dialog.setOnConfirmListener((name, description) -> {
            if (name.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "地点名称和描述不应为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Node node = new Node(latLng, name, description);

            MarkerOptions options = new MarkerOptions(latLng);
            options.infoWindowEnable(false);
            options.title(name)
                    .snippet(description);
            Marker marker = tencentMap.addMarker(options);

            marker.setInfoWindowEnable(true);
            marker.setClickable(true);

            markerToNode.put(marker, node);
            markerToPolyline.put(marker, new ArrayList<>());
            graph.addNode(node);
        });

        dialog.show(getSupportFragmentManager(), "EditDialog");
    };

    private TencentMap.OnMarkerClickListener nodeMarkerCL = marker -> {
        /* 点击显示地点信息 提供更改和删除功能 */
        Node node = markerToNode.get(marker);
        if (isShow) marker.hideInfoWindow(); else marker.showInfoWindow();

        button_1.setText("更改");
        button_2.setText("删除");

        button_1.setOnClickListener(v -> {
            /* 更改 */
            EditDialogFragment dialog = new EditDialogFragment();

            dialog.setOnConfirmListener((name, description) -> {
                if (name.isEmpty() || description.isEmpty()) {
                    Toast.makeText(this, "地点名称和描述不应为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                node.setName(name);
                node.setDescription(description);

                marker.getOptions().title(name);
                marker.getOptions().snippet(description);

                marker.showInfoWindow();
            });

            dialog.show(getSupportFragmentManager(), "EditDialog");
        });

        button_2.setOnClickListener(v -> {
            /* 删除 */
            Node temp = markerToNode.get(marker);
            markerToNode.remove(marker);

            List<Polyline> polylines = markerToPolyline.get(marker);
            for (Polyline polyline : polylines)
                polyline.remove();
            markerToPolyline.remove(marker);

            graph.removeNode(temp.id);

            marker.remove();

            isShow = !isShow;
            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);

            Toast.makeText(this, "地点标记已删除", Toast.LENGTH_SHORT).show();
        });

        if (isShow) {
            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);
        } else {
            button_1.setVisibility(View.VISIBLE);
            button_2.setVisibility(View.VISIBLE);
        }

        isShow = !isShow;

        return true;
    };

    private TencentMap.OnMapClickListener viewMapCL = latLng -> {
        /* 通常没有功能 */
    };

    private TencentMap.OnMarkerClickListener viewMarkerCL = marker -> {
        /* 点击显示地点信息 提供from-to功能 */
        Node node = markerToNode.get(marker);
        if (isShow) marker.hideInfoWindow(); else marker.showInfoWindow();

        button_1.setText("从这来");
        button_2.setText("到这去");

        button_1.setOnClickListener(v -> {
            /* from */
            viewStart = marker;

            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);
            marker.hideInfoWindow();
            isShow = !isShow;
        });

        button_2.setOnClickListener(v -> {
            /* to */
            if (viewStart == null) {
                Toast.makeText(this, "请选择起点", Toast.LENGTH_SHORT).show();
                return;
            } else if (viewStart == marker) {
                Toast.makeText(this, "终点和起点不应该是同一地点", Toast.LENGTH_SHORT).show();
                return;
            }

            Node from = markerToNode.get(viewStart);
            Node to = markerToNode.get(marker);
            double distance = graph.getShortestPath(from.id, to.id);
            Toast.makeText(this, "最近距离是: " + (int) distance + "米", Toast.LENGTH_SHORT).show();

            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);
            marker.hideInfoWindow();
            isShow = !isShow;
            viewStart = null;
        });

        if (isShow) {
            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);
        } else {
            button_1.setVisibility(View.VISIBLE);
            button_2.setVisibility(View.VISIBLE);
        }

        isShow = !isShow;

        return true;
    };

    private TencentMap.OnMapClickListener edgeMapCL = latLng -> {
        /* 点击区域选取途经点 */
        MarkerOptions mp = new MarkerOptions(latLng);
        BitmapDescriptor custom = BitmapDescriptorFactory.fromResource(R.drawable.waypoint);
        mp.icon(custom);
        Marker marker = tencentMap.addMarker(mp);
        routePoints.add(marker);
    };

    private TencentMap.OnMarkerClickListener edgeMarkerCL = marker -> {
        /* 首次点击Marker作为起点 再次点击其他Marker生成道路 */
        if (edgeStart == null) {
            edgeStart = marker;
            for (Marker m : routePoints) m.remove();
            routePoints.clear();
        } else {
            Pair<Double, Polyline> pair = drawRoute(edgeStart, marker);
            double distance = pair.first;
            Polyline polyline = pair.second;

            markerToPolyline.get(edgeStart).add(polyline);
            markerToPolyline.get(marker).add(polyline);

            Node from = markerToNode.get(edgeStart);
            Node to = markerToNode.get(marker);

            Edge edge1 = new Edge(from.id, to.id, distance);
            Edge edge2 = new Edge(to.id, from.id, distance);

            graph.addEdge(edge1);
            graph.addEdge(edge2);

            routePoints.clear();
            edgeStart = null;
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

        TencentMapInitializer.setAgreePrivacy(true); /* 默认用户同意腾讯地图隐私协议 */

        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(new LatLng(34.15711406, 108.90797981));
        latLngs.add(new LatLng(34.14858452597058, 108.8977919290345));
        bounds = LatLngBounds.builder()
                .include(latLngs)
                .build(); /* 学校范围 */
        boundsCenter = bounds.getCenter();

        tencentMap = map.getMap();
        tencentMap.getUiSettings().setRotateGesturesEnabled(false);
        tencentMap.getUiSettings().setTiltGesturesEnabled(false); /* 禁止倾斜面和俯仰角手势 */
        tencentMap.setMapType(TencentMap.MAP_TYPE_NONE);

        /* 初始化相机 限制最大放缩能力 */
        tencentMap.setOnMapLoadedCallback(() -> {
            tencentMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding)
            );

            map.post(() -> {
                float fixedZoom = tencentMap.getCameraPosition().zoom;
                tencentMap.setMinZoomLevel((int) fixedZoom);
            });
        });

        /* 相机归正处理 */
        tencentMap.setOnCameraChangeListener(
                new TencentMap.OnCameraChangeListener() {

                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {}

                    @Override
                    public void onCameraChangeFinished(CameraPosition cameraPosition) {
                        if (correcting) return;

                        LatLng screenCenter = cameraPosition.target;

                        double latSpan = bounds.getNorthEast().latitude
                                - bounds.getSouthWest().latitude;
                        double lngSpan = bounds.getNorthEast().longitude
                                - bounds.getSouthWest().longitude;

                        double latOffset = Math.abs(
                                screenCenter.latitude - boundsCenter.latitude
                        );
                        double lngOffset = Math.abs(
                                screenCenter.longitude - boundsCenter.longitude
                        );

                        if (latOffset > latSpan * 0.5
                                || lngOffset > lngSpan * 0.5) {
                            reformat();
                        }
                    }
                }
        );

        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.post(() -> {
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setExpandedOffset(0);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        toggleGroup = findViewById(R.id.mode_toggle_group);

        toggleGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }

                    if (checkedId == R.id.view_mode) {
                        /* 仅观察模式 */

                        reset();
//                        Toast.makeText(this, "当前处于地图浏览模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(viewMapCL);
                        tencentMap.setOnMarkerClickListener(viewMarkerCL);
                    } else if (checkedId == R.id.node_mode) {
                        /* 编辑地点模式 */

                        reset();
//                        Toast.makeText(this, "当前处于编辑节点模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(nodeMapCL);
                        tencentMap.setOnMarkerClickListener(nodeMarkerCL);
                    } else if (checkedId == R.id.edge_mode) {
                        /* 编辑道路模式 */

                        reset();
//                        Toast.makeText(this, "当前处于编辑道路模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(edgeMapCL);
                        tencentMap.setOnMarkerClickListener(edgeMarkerCL);
                    }
                }
        );

        button_1 = findViewById(R.id.button_1);
        button_2 = findViewById(R.id.button_2);

        button_1.setVisibility(View.GONE);
        button_2.setVisibility(View.GONE);

        toggleGroup.check(R.id.view_mode);
    }

    private void reformat() {
        correcting = true;

        tencentMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding),
                new TencentMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        correcting = false;
                    }

                    @Override
                    public void onCancel() {
                        correcting = false;
                    }
                }
        );
    }

    private void reset() {
        if (isShow) {
            button_1.setVisibility(View.GONE);
            button_2.setVisibility(View.GONE);

            isShow = !isShow;
        }

        for (Marker marker : markerToNode.keySet())
            marker.hideInfoWindow();

        for (Marker marker : routePoints)
            marker.remove();
        routePoints.clear();
    }

    private Pair<Double, Polyline> drawRoute(Marker start, Marker end) {
        double totalDistance = 0;

        routePoints.addFirst(start);
        routePoints.addLast(end);

        for (int i = 0; i < routePoints.size() - 1; i++) {
            totalDistance += calculateDistance(
                    routePoints.get(i).getPosition(),
                    routePoints.get(i + 1).getPosition());
        }

        List<LatLng> list = routePoints.stream().map(marker -> marker.getPosition()).toList();

        PolylineOptions polylineOptions = new PolylineOptions().addAll(list)
                .width(6);
        Polyline polyline = tencentMap.addPolyline(polylineOptions);

//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
//        for (LatLng point : list) {
//            builder.include(point);
//        }
//        LatLngBounds bounds = builder.build();
//        tencentMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        for (int i = 1; i < routePoints.size() - 1; ++i) {
            routePoints.get(i).remove();
        }

        return new Pair<>(totalDistance, polyline);
    }

    public double calculateDistance(LatLng startPoint, LatLng endPoint) {
        double R = 6371000;
        double lat1 = Math.toRadians(startPoint.latitude);
        double lon1 = Math.toRadians(startPoint.longitude);
        double lat2 = Math.toRadians(endPoint.latitude);
        double lon2 = Math.toRadians(endPoint.longitude);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @Override
    protected void onStart() {
        super.onStart();
        map.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        map.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
    }
}