/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.wheelmap.android.activity.MapActivity;
import org.wheelmap.android.app.WheelmapApp;
import org.wheelmap.android.fragment.SearchDialogFragment.OnSearchDialogListener;
import org.wheelmap.android.manager.MyLocationManager;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.Wheelmap.POIs;
import org.wheelmap.android.online.BuildConfig;
import org.wheelmap.android.online.R;
import org.wheelmap.android.osmdroid.MarkItemOverlay;
import org.wheelmap.android.osmdroid.MyLocationNewOverlayFixed;
import org.wheelmap.android.osmdroid.OnTapListener;
import org.wheelmap.android.osmdroid.POIsCursorOsmdroidOverlay;
import org.wheelmap.android.utils.MyLocationProvider;
import org.wheelmap.android.utils.ParceableBoundingBox;
import org.wheelmap.android.utils.PressSelector;
import org.wheelmap.android.utils.UtilsMisc;
import java.util.Locale;
import de.akquinet.android.androlog.Log;
import de.greenrobot.event.EventBus;
import static org.wheelmap.android.utils.PressSelector.setAlphaForView;

public class POIsOsmdroidFragment extends Fragment implements
        DisplayFragment, MapListener, OnTapListener,
        OnSearchDialogListener, OnExecuteBundle {

    public final static String TAG = POIsOsmdroidFragment.class
            .getSimpleName();

    private static final float SPAN_ENLARGEMENT_FAKTOR = 1.3f;

    private static final byte ZOOMLEVEL_MIN = 16;
    private static final byte ZOOMLEVEL_MAX = 15;

    private static final int MAP_ZOOM_DEFAULT = 18; // Zoon 1 is world view

    private static String baseUrl = "http://a.tiles.mapbox.com/v3/%s/";

    private static String tileUrl;

    private OnlineTileSourceBase mMapBoxTileSource;

    private WorkerFragment mWorkerFragment;

    private FrameLayout txtOutOfZoom;

    private DisplayFragmentListener mListener;

    private Bundle mDeferredExecuteBundle;

    private MapView mMapView;

    private IMapController mMapController;

    private POIsCursorOsmdroidOverlay mPoisItemizedOverlay;

    private MyLocationNewOverlay mCurrLocationOverlay;

    private MarkItemOverlay markItemOverlay;

    private IGeoPoint mLastRequestedPosition;

    private GeoPoint mCurrentLocationGeoPoint;

    private boolean mHeightFull = true;

    private boolean isCentered = false;

    private int oldZoomLevel = MAP_ZOOM_DEFAULT;

    private Cursor mCursor;

    private Location mLocation;

    private EventBus mBus;

    private MyLocationNewOverlay mMyLocationOverlay;
    private MyLocationProvider mMyLocationProvider = new MyLocationProvider();

    private ImageButton mBtnLocate;

    public POIsOsmdroidFragment() {
        // noop
    }

    public static POIsOsmdroidFragment newInstance(boolean createWorker,
                                                   boolean disableSearch) {
        createWorker = false;
        POIsOsmdroidFragment f = new POIsOsmdroidFragment();
        Bundle b = new Bundle();
        b.putBoolean(Extra.CREATE_WORKER_FRAGMENT, createWorker);
        b.putBoolean(Extra.DISABLE_SEARCH, disableSearch);

        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof DisplayFragmentListener) {
            mListener = (DisplayFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        tileUrl = String.format(Locale.US, baseUrl, BuildConfig.MAPBOX_API_KEY);
        mMapBoxTileSource = new XYTileSource("Mapbox", 3, 21, 256, ".png", new String[]{tileUrl});
        mBus = EventBus.getDefault();

        attachWorkerFragment();
        retrieveInitialLocation();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater
                .inflate(R.layout.fragment_osmdroid, container, false);

        txtOutOfZoom = (FrameLayout) v.findViewById(R.id.my_outofzoom_text_smartphone);

        if (UtilsMisc.isTablet(getActivity().getApplication())) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                txtOutOfZoom.setVisibility(View.GONE);
                txtOutOfZoom = (FrameLayout) getActivity().findViewById(R.id.my_outofzoom_text_tablet_portrait);
                try {
                    setAlphaForView(txtOutOfZoom, (float) 0.5);
                } catch (NullPointerException npex) {
                    Log.d("Tag:POIsOsmdroidFragment", "NullPointException occurred");

                    Toast.makeText(this.getActivity().getApplicationContext(),
                            getResources().getString(R.string.error_internal_error), Toast.LENGTH_LONG).show();
                }
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                txtOutOfZoom.setVisibility(View.GONE);
                txtOutOfZoom = (FrameLayout) getActivity().findViewById(R.id.my_outofzoom_text_tablet_landscape);

                try {
                    setAlphaForView(txtOutOfZoom, (float) 0.5);
                } catch (NullPointerException npex) {
                    Log.d("Tag:POIsOsmdroidFragment", "NullPointException occurred");

                    Toast.makeText(this.getActivity().getApplicationContext(),
                            getResources().getString(R.string.error_internal_error), Toast.LENGTH_LONG).show();
                }
            }
        }

        try {
            txtOutOfZoom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    txtOutOfZoom.setVisibility(View.GONE);
                    zoomInToMax();
                }
            });
        } catch (NullPointerException npex) {
            Log.d("Tag:POIsOsmdroidFragment", "NullPointException occurred");

            Toast.makeText(this.getActivity().getApplicationContext(),
                    getResources().getString(R.string.error_internal_error), Toast.LENGTH_LONG).show();
        }

        mMapView = (MapView) v.findViewById(R.id.map);
        mMapView.setTileSource(mMapBoxTileSource);
        setHardwareAccelerationOff();
        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestUpdate();
            }
        }, 1000);

        mMapController = mMapView.getController();

        // overlays
        mPoisItemizedOverlay = new POIsCursorOsmdroidOverlay(getActivity(), this);
        mCurrLocationOverlay = new MyLocationNewOverlayFixed(mMyLocationProvider,
                mMapView);


        mCurrLocationOverlay.enableFollowLocation();
        mCurrLocationOverlay.enableMyLocation();

        markItemOverlay = new MarkItemOverlay(getActivity(), mMapView);

        mMapView.getOverlays().add(markItemOverlay);

        mMapView.getOverlays().add(mPoisItemizedOverlay);

        mMyLocationOverlay = new MyLocationNewOverlayFixed(mMyLocationProvider, mMapView);

        mMyLocationOverlay.disableFollowLocation();
        mMapView.getOverlays().add(mMyLocationOverlay);

        mMyLocationProvider.startLocationProvider(mMyLocationOverlay);
        mMyLocationOverlay.enableMyLocation();

        mMapView.setMapListener(this);

        mBtnLocate = (ImageButton) v.findViewById(R.id.map_btn_locate);
        mBtnLocate.setOnTouchListener(new PressSelector());
        mBtnLocate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mMapView.getZoomLevel(true) <= ZOOMLEVEL_MAX) {
                    setZoomIntern(MAP_ZOOM_DEFAULT);
                    txtOutOfZoom.setVisibility(View.GONE);
                }
                centerMap(mCurrentLocationGeoPoint, true);
                requestUpdate();
            }
        });

        onRestoreInstanceState(savedInstanceState);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onEventMainThread(mBus.getStickyEvent(MyLocationManager.LocationEvent.class));
    }

    public void zoomInToMax() {
        setZoomIntern(MAP_ZOOM_DEFAULT);
        requestUpdate();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MapActivity) getActivity()).registerMapView(mMapView);
        executeConfig(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mBus.registerSticky(this);
        mBus.post(MyLocationManager.RegisterEvent.INSTANCE);
        mMyLocationProvider.register();
    }

    @Override
    public void onResume() {
        super.onResume();
        executeState(retrieveExecuteBundle());
    }

    @Override
    public void onDetach() {
        mMapView.onDetach();
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBus.post(new MyLocationManager.UnregisterEvent());
        mBus.unregister(this);
        mMyLocationProvider.unregister();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((MapActivity) getActivity()).unregisterMapView(mMapView);
        mWorkerFragment.unregisterDisplayFragment(this);
        WheelmapApp.getSupportManager().cleanReferences();
    }

    private void retrieveInitialLocation() {
        MyLocationManager.LocationEvent event = (MyLocationManager.LocationEvent) mBus
                .getStickyEvent(MyLocationManager.LocationEvent.class);
        mLocation = event.location;
        Log.d("retrieveInitialLocation: mLocation = " + mLocation);
    }

    private void attachWorkerFragment() {
        FragmentManager fm = getFragmentManager();
        Fragment fragment = (Fragment) fm.findFragmentByTag(CombinedWorkerFragment.TAG);
        mWorkerFragment = (WorkerFragment) fragment;
        mWorkerFragment.registerDisplayFragment(this);
        Log.d(TAG, "result mWorkerFragment = " + mWorkerFragment);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void executeConfig(Bundle savedInstanceState) {
        if (hasExecuteBundle()) {
            Log.d(TAG, "executeConfig: initialized from execute bundle");
            return;
        }
        if (savedInstanceState != null) {
            Log.d(TAG, "executeConfig: initialized from savedInstanceState");
            executeBundle(savedInstanceState);
            return;
        }

        if (((MapActivity) getActivity()).loadPreferences(mMapView)) {
            Log.d(TAG, "executeConfig: initialized from preferences");
            return;
        }

        Log.d(TAG, "executeConfig: initialized from defaults");
        if (mLocation == null) {
            return;
        }
        executeMapPositioning(new GeoPoint(mLocation), MAP_ZOOM_DEFAULT);
    }

    @Override
    public void executeBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        Log.d(TAG, "executeBundle fragment is visible = " + isVisible());
        if (isVisible()) {
            bundle.putBoolean(Extra.EXPLICIT_DIRECT_RETRIEVAL, true);
            executeState(bundle);
        } else {
            storeExecuteBundle(bundle);
        }
    }

    private void executeState(Bundle bundle) {
        if (bundle == null) {
            return;
        }

        mHeightFull = bundle.getBoolean(Extra.MAP_HEIGHT_FULL, false);

        boolean doRequest = false;
        boolean doCenter = false;
        GeoPoint centerPoint = null;
        int zoom = 0;

        if (bundle.containsKey(Extra.REQUEST)) {
            doRequest = true;
        }

        if (bundle.containsKey(Extra.CENTER_MAP)) {
            double lat = bundle.getDouble(Extra.LATITUDE);
            double lon = bundle.getDouble(Extra.LONGITUDE);
            zoom = bundle.getInt(Extra.ZOOM_MAP, MAP_ZOOM_DEFAULT);
            centerPoint = new GeoPoint(lat, lon);
            doCenter = true;
        }

        if (doCenter || doRequest) {
            executeMapDeferred(centerPoint, zoom, doCenter, doRequest);
        }
    }

    private boolean hasExecuteBundle() {
        return mDeferredExecuteBundle != null;
    }

    private void storeExecuteBundle(Bundle bundle) {
        mDeferredExecuteBundle = bundle;
    }

    private Bundle retrieveExecuteBundle() {
        Bundle result = mDeferredExecuteBundle;
        mDeferredExecuteBundle = null;
        return result;
    }

    private void executeMapDeferred(final GeoPoint centerPoint, final int zoom,
                                    final boolean center, final boolean request) {
        mMapView.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        Log.d("onGlobalLayout: doing mapview center deffered");
                        if (center) {
                            executeMapPositioning(centerPoint, zoom);
                        }
                        if (request) {
                            requestUpdate();
                        }

                        mMapView.getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                    }
                });
    }

    private void executeMapPositioning(GeoPoint geoPoint, int zoom) {
        if (geoPoint != null) {
            centerMap(geoPoint, true);
        }
        if (zoom != 0) {
            setZoomIntern(zoom);
        }
        markItemIntern(geoPoint, false);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        int la = savedInstanceState.getInt(Extra.LATITUDE);
        int lo = savedInstanceState.getInt(Extra.LONGITUDE);
        int zoom = savedInstanceState.getInt(Extra.ZOOM_LEVEL);

        mCurrentLocationGeoPoint = new GeoPoint(la, lo);

        setZoomIntern(zoom);

        centerMap(mCurrentLocationGeoPoint, true);

        if (savedInstanceState.containsKey(Extra.SELECTED_LATITUDE)) {
            Location selectedLocation = new Location("gps");
            selectedLocation.setLongitude(savedInstanceState.getDouble(Extra.SELECTED_LONGITUDE));
            selectedLocation.setLatitude(savedInstanceState.getDouble(Extra.SELECTED_LATITUDE));
            markItemOverlay.setLocation(selectedLocation);
        }

        requestUpdate();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: executing");
        outState.putBoolean(Extra.MAP_HEIGHT_FULL, mHeightFull);

        IGeoPoint current_location = mMapView.getMapCenter();
        int zoomlevel = mMapView.getZoomLevel();

        outState.putInt(Extra.ZOOM_LEVEL, zoomlevel);
        outState.putInt(Extra.LATITUDE, current_location.getLatitudeE6());
        outState.putInt(Extra.LONGITUDE, current_location.getLongitudeE6());

        GeoPoint selectedPOI = markItemOverlay.getLocation();
        if (selectedPOI != null) {
            outState.putDouble(Extra.SELECTED_LATITUDE, selectedPOI.getLatitude());
            outState.putDouble(Extra.SELECTED_LONGITUDE, selectedPOI.getLongitude());
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ab_map_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_search:
                showSearch();
                return true;
        }

        return false;
    }

    @Override
    public void onRefreshStarted() {
    }

    @Override
    public boolean onScroll(ScrollEvent event) {
        Log.v(TAG, "onScroll");
        IGeoPoint centerLocation = mMapView.getMapCenter();
        int minimalLatitudeSpan = mMapView.getLatitudeSpan() / 3;
        int minimalLongitudeSpan = mMapView.getLongitudeSpan() / 3;

        if (mLastRequestedPosition != null
                && (Math.abs(mLastRequestedPosition.getLatitudeE6()
                - centerLocation.getLatitudeE6()) < minimalLatitudeSpan)
                && (Math.abs(mLastRequestedPosition.getLongitudeE6()
                - centerLocation.getLongitudeE6()) < minimalLongitudeSpan)) {
            return false;
        }

        if (mMapView.getZoomLevel() < ZOOMLEVEL_MIN) {
            return false;
        }

        Log.d(TAG, "onScroll: requirements passed - requesting update");
        requestUpdate();
        return true;
    }

    @Override
    public boolean onZoom(ZoomEvent event) {
        int zoomLevel = event.getZoomLevel();
        Log.d(TAG, "onZoom: " + zoomLevel);
        boolean isZoomedEnough = true;

        if (zoomLevel <= ZOOMLEVEL_MAX) {
            txtOutOfZoom.setVisibility(View.VISIBLE);
        } else {
            txtOutOfZoom.setVisibility(View.GONE);
        }

        if (zoomLevel < ZOOMLEVEL_MIN) {
            oldZoomLevel = zoomLevel;
            return false;
        }

        if (zoomLevel < oldZoomLevel) {
            isZoomedEnough = false;
        }

        if (isZoomedEnough && zoomLevel >= oldZoomLevel) {
            oldZoomLevel = zoomLevel;
            return false;
        }

        requestUpdate();
        oldZoomLevel = zoomLevel;
        return false;
    }

    protected void requestUpdate() {
        Bundle extras = fillExtrasWithBoundingRect();
        if (extras == null) {
            return;
        }
        mWorkerFragment.requestUpdate(extras);
    }

    private Bundle fillExtrasWithBoundingRect() {
        Bundle bundle = new Bundle();

        int latSpan = (int) (mMapView.getLatitudeSpan() * SPAN_ENLARGEMENT_FAKTOR);
        int lonSpan = (int) (mMapView.getLongitudeSpan() * SPAN_ENLARGEMENT_FAKTOR);
        if (latSpan <= 0 || lonSpan <= 0) {
            //mapview is not fully loaded
            return null;
        }
        IGeoPoint center = mMapView.getMapCenter();
        mLastRequestedPosition = center;
        ParceableBoundingBox boundingBox = new ParceableBoundingBox(
                center.getLatitudeE6() + (latSpan / 2), center.getLongitudeE6()
                + (lonSpan / 2),
                center.getLatitudeE6() - (latSpan / 2), center.getLongitudeE6()
                - (lonSpan / 2));
        bundle.putSerializable(Extra.BOUNDING_BOX, boundingBox);

        return bundle;
    }

    private void centerMap(GeoPoint geoPoint, boolean force) {
        centerMap(geoPoint, force, true);
    }

    private void centerMap(GeoPoint geoPoint, boolean force, boolean withOffset) {
        Log.d(TAG, "centerMap: force = " + force + " isCentered = "
                + isCentered + " geoPoint = " + geoPoint);
        if (force || !isCentered) {
            if (withOffset) {
                setCenterWithOffset(geoPoint);
            } else {
                mMapController.setCenter(geoPoint);
                isCentered = true;
            }
        }
    }

    private void setCenterWithOffset(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return;
        }
        Log.d(TAG, "setCenterWithOffset");
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        IGeoPoint actualGeoPoint = geoPoint;

        if (mHeightFull) {
            actualGeoPoint = geoPoint;
        } else {

            if (land) {
                Point point = pointFromGeoPoint(geoPoint, mMapView);
                int horizontalOffset = mMapView.getWidth() / 4;
                point.x -= horizontalOffset;
                actualGeoPoint = geoPointFromScreenCoords(point.x, point.y, mMapView);

            } else {
                Point point = pointFromGeoPoint(geoPoint, mMapView);
                int mVerticalOffset = mMapView.getHeight() / 4;
                point.y -= mVerticalOffset;
                actualGeoPoint = geoPointFromScreenCoords(point.x, point.y, mMapView);
            }
        }

        mMapController.setCenter(actualGeoPoint);
        isCentered = true;
    }

    /**
     * @param x  view coord relative to left
     * @param y  view coord relative to top
     * @param vw MapView
     * @return GeoPoint
     */
    private GeoPoint geoPointFromScreenCoords(int x, int y, MapView vw) {
        // Get the top left GeoPoint
        Projection projection = vw.getProjection();
        GeoPoint geoPointTopLeft = (GeoPoint) projection.fromPixels(0, 0);
        Point topLeftPoint = new Point();
        // Get the top left Point (includes osmdroid offsets)
        projection.toPixels(geoPointTopLeft, topLeftPoint);
        // get the GeoPoint of any point on screen
        GeoPoint rtnGeoPoint = (GeoPoint) projection.fromPixels(x, y);
        return rtnGeoPoint;
    }

    /**
     * @param gp GeoPoint
     * @param vw Mapview
     * @return a 'Point' in screen coords relative to top left
     */
    private Point pointFromGeoPoint(GeoPoint gp, MapView vw) {

        Point rtnPoint = new Point();
        Projection projection = vw.getProjection();
        projection.toPixels(gp, rtnPoint);
        // Get the top left GeoPoint
        GeoPoint geoPointTopLeft = (GeoPoint) projection.fromPixels(0, 0);
        Point topLeftPoint = new Point();
        // Get the top left Point (includes osmdroid offsets)
        projection.toPixels(geoPointTopLeft, topLeftPoint);
        rtnPoint.x -= topLeftPoint.x; // remove offsets
        rtnPoint.y -= topLeftPoint.y;
        return rtnPoint;
    }

    public void setHeightFull(boolean heightFull) {
        mHeightFull = heightFull;
    }

    private void setZoomIntern(int zoom) {
        Log.d(TAG, "setZoomInternal: " + zoom);
        mMapView.setMapListener(null);
        mMapController.setZoom(zoom);
        mMapView.setMapListener(this);
        oldZoomLevel = zoom;
    }

    private void setCursor(Cursor cursor) {
        //UtilsMisc.dumpCursorCompare(TAG, mCursor, cursor);
        if (cursor == mCursor) {
            return;
        }

        mCursor = cursor;
        mPoisItemizedOverlay.setCursor(mCursor);
        mMapView.postInvalidate();
    }

    @Override
    public void onTap(OverlayItem item, ContentValues values) {
        markItem(values, false);
    }

    private void showSearch() {
        FragmentManager fm = getFragmentManager();
        SearchDialogFragment searchDialog = SearchDialogFragment.newInstance(
                false, true);

        searchDialog.setTargetFragment(this, 0);
        searchDialog.show(fm, getTag());
    }

    @Override
    public void onSearch(Bundle bundle) {
        Bundle boundingBoxExtras = fillExtrasWithBoundingRect();
        bundle.putAll(boundingBoxExtras);

        mWorkerFragment.requestSearch(bundle);
    }

    @Override
    public void onUpdate(WorkerFragment fragment) {
        setCursor(fragment.getCursor(WorkerFragment.MAP_CURSOR));

        if (mListener != null) {
            mListener.onRefreshing(fragment.isRefreshing());
        }
    }

    public void onEventMainThread(MyLocationManager.LocationEvent locationEvent) {
        if (!locationEvent.isValid) {
            if (mMapView != null && !isCentered) {
                mMapView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        centerMap(new GeoPoint(MyLocationManager.DEFAULT_LATITUDE, MyLocationManager.DEFAULT_LONGITUDE), false);
                        setZoomIntern(7);
                        mMapView.getOverlays().remove(mMyLocationOverlay);
                    }
                }, 100);
            }
            return;
        }
        if (mMapView != null) {
            if (!mMapView.getOverlays().contains(mMyLocationOverlay)) {
                mMapView.getOverlays().add(mMyLocationOverlay);
            }
        }
        mLocation = locationEvent.location;

        Log.d(TAG, "updateLocation: " + mLocation);
        GeoPoint geoPoint = new GeoPoint(mLocation.getLatitude(),
                mLocation.getLongitude());

        if (mMapView != null && !isCentered) {
            mMyLocationOverlay.enableFollowLocation();
            mMapView.getOverlays().add(mMyLocationOverlay);
            mMyLocationProvider.updateLocation(mLocation);
            mMyLocationOverlay.disableFollowLocation();
            isCentered = true;
        }

        mCurrentLocationGeoPoint = geoPoint;
        mMyLocationProvider.updateLocation(mLocation);
    }

    private Location getLocation() {
        return mLocation;
    }

    @Override
    public void markItem(ContentValues values, boolean centerToItem) {
        Log.d(TAG, "markItem " + values);
        GeoPoint point = new GeoPoint(values.getAsDouble(POIs.LATITUDE),
                values.getAsDouble(POIs.LONGITUDE));
        markItemIntern(point, centerToItem);
        WheelmapApp app = (WheelmapApp) this.getActivity().getApplication();
        boolean b = app.isNoItemToSelect();
        app.setNoItemToSelect(false);
        if (!b || !UtilsMisc.isTablet(app)) {

            if (mListener != null) {
                mListener.onShowDetail(this, values);
            }
        }
    }

    private void markItemIntern(GeoPoint point, boolean centerToItem) {
        markItemOverlay.setLocation(point);
        if (centerToItem) {
            centerMap(point, true);
        }
    }
}
