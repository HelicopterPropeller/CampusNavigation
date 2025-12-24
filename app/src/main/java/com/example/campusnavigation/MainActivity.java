package com.example.campusnavigation;

import android.os.Bundle;
import android.view.View;
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

import java.util.ArrayList;
import java.util.HashMap;
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
    private Map<Marker, Node> markerToNode = new HashMap<>();
    private Map<Marker, Edge> markerToEdge = new HashMap<>();
    private boolean isShow = false;
    private MaterialButtonToggleGroup toggleGroup;
    private View bottomSheet;

    private TencentMap.OnMapClickListener nodeMapCL = latLng -> {
        /* 点击设立地点 弹出弹窗以编辑地点信息 */
        EditDialogFragment dialog = new EditDialogFragment();

        dialog.setOnConfirmListener(name -> {
            Node node = new Node(latLng, name);

            MarkerOptions options = new MarkerOptions(latLng);
            options.infoWindowEnable(false);
            options.title("天安门")
                    .snippet("地址: 北京市东城区东长安街");
            Marker marker = tencentMap.addMarker(options);

            marker.setInfoWindowEnable(true);
            marker.setClickable(true);

            markerToNode.put(marker, node);
        });

        dialog.show(getSupportFragmentManager(), "EditDialog");
    };

    private TencentMap.OnMarkerClickListener nodeMarkerCL = marker -> {
        /* 点击显示地点信息 提供更改和删除功能 */
        if(markerToNode.containsKey(marker)) {
            Node node = markerToNode.get(marker);

            if (isShow) marker.hideInfoWindow(); else marker.showInfoWindow();

            isShow = !isShow;
        }
        return true;
    };

    private TencentMap.OnMapClickListener viewMapCL = latLng -> {
        /* 通常没有功能 */
    };

    private TencentMap.OnMarkerClickListener viewMarkerCL = marker -> {
        /* 点击显示地点信息和from-to按钮 */
        return true;
    };

    private TencentMap.OnMapClickListener edgeMapCL = latLng -> {
        /* 点击区域选取途经点 */
    };

    private TencentMap.OnMarkerClickListener edgeMarkerCL = marker -> {
        /* 首次点击Marker作为起点 再次点击其他Marker生成道路 */
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

                        reformat();
                        Toast.makeText(this, "当前处于地图浏览模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(viewMapCL);
                        tencentMap.setOnMarkerClickListener(viewMarkerCL);
                    } else if (checkedId == R.id.node_mode) {
                        /* 编辑地点模式 */

                        reformat();
                        Toast.makeText(this, "当前处于编辑节点模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(nodeMapCL);
                        tencentMap.setOnMarkerClickListener(nodeMarkerCL);
                    } else if (checkedId == R.id.edge_mode) {
                        /* 编辑道路模式 */

                        reformat();
                        Toast.makeText(this, "当前处于编辑道路模式", Toast.LENGTH_SHORT).show();
                        tencentMap.setOnMapClickListener(edgeMapCL);
                        tencentMap.setOnMarkerClickListener(edgeMarkerCL);
                    }
                }
        );

        Toast.makeText(this, "当前处于地图浏览模式", Toast.LENGTH_SHORT).show();
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