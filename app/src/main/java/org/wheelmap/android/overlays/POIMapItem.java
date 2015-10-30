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
package org.wheelmap.android.overlays;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.wheelmap.android.model.WheelchairFilterState;

import android.graphics.drawable.Drawable;

@Deprecated
public class POIMapItem extends OverlayItem {

    /**
     * Stores the wheelchair state
     */
    private final WheelchairFilterState mState;

    /**
     * Stores the unique id
     */
    private final int id;


    /**
     * Constructs a new GeoPoint with the given latitude and longitude, measured in degrees.
     *
     * @param point The GeoPoint of POI
     * @param state wheelchair state.
     */
    public POIMapItem(GeoPoint point, WheelchairFilterState state, int id, Drawable marker) {
        super();
        this.mState = state;
        this.id = id;
        setPoint(point);
        setMarker(marker);
    }

    /**
     * Returns the WheelchairState
     *
     * @return the WheelchairState of the POI
     */
    public WheelchairFilterState getWheelchairState() {
        return this.mState;
    }

    /**
     * Returns id
     *
     * @return id of the POI
     */
    public int getId() {
        return this.id;
    }
}
