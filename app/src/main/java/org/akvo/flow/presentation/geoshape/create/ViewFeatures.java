/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.presentation.geoshape.create;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

import androidx.annotation.NonNull;

public class ViewFeatures {

    public static final String POINT_SELECTED_PROPERTY = "is-point-selected";
    public static final String SHAPE_SELECTED_PROPERTY = "is-shape-selected";
    public static final String POINT_ID = "point-id";
    public static final String FEATURE_ID = "shape-id";
    public static final String FEATURE_LINE = "is-line";
    public static final String FEATURE_POINT = "is-point";
    public static final String FEATURE_POLYGON = "is-polygon";

    private final List<Feature> features;
    private final List<Feature> pointFeatures;
    private final List<LatLng> listOfCoordinates;

    public ViewFeatures(@NonNull List<Feature> features, @NonNull  List<Feature> pointFeatures,
            @NonNull List<LatLng> listOfCoordinates) {
        this.features = features;
        this.pointFeatures = pointFeatures;
        this.listOfCoordinates = listOfCoordinates;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<Feature> getPointFeatures() {
        return pointFeatures;
    }

    public List<LatLng> getListOfCoordinates() {
        return listOfCoordinates;
    }
}
