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
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.geoshape.entities.AreaShape;
import org.akvo.flow.presentation.geoshape.entities.FeatureMapper;
import org.akvo.flow.presentation.geoshape.entities.LineShape;
import org.akvo.flow.presentation.geoshape.entities.PointShape;
import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ShapePoint;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class CreateGeoShapePresenter implements Presenter {

    private final FeatureMapper featureMapper;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());
    private final List<Shape> shapes = new ArrayList<>();
    private CreateGeoShapeView view;

    @Inject
    public CreateGeoShapePresenter(FeatureMapper featureMapper) {
        this.featureMapper = featureMapper;
    }

    @Override
    public void destroy() {
        //EMPTY
    }

    public void setView(CreateGeoShapeView view) {
        this.view = view;
    }

    public void setUpFeatures(String geoJSON) {
        shapes.clear();
        shapes.addAll(featureMapper.toShapes(geoJSON));
        viewFeatures = featureMapper.toViewFeatures(shapes);
    }

    public void onShapeInfoPressed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            view.displaySelectedShapeInfo(shape);
        }
    }

    public void onDeletePointPressed() {
        if (getSelectedShape() != null) {
            view.displayDeletePointDialog();
        }
    }

    public void onDeleteShapePressed() {
        if (getSelectedShape() != null) {
            view.displayDeleteShapeDialog();
        }
    }

    public void onMapReady() {
        view.displayMapItems(viewFeatures);
    }

    public boolean onMapClick(Feature feature) {
        Shape selected = selectFeatureFromPoint(feature);
        if (selected instanceof PointShape) {
            view.enablePointDrawMode();
        } else if (selected instanceof LineShape) {
            view.enableLineDrawMode();
        } else if (selected instanceof AreaShape) {
            view.enableAreaDrawMode();
        }
        updateSources();
        return true;
    }

    public void onAddPointRequested(LatLng latLng, DrawMode drawMode) {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shape.addPoint(latLng);
        } else {
            unSelectAllFeatures();
            Shape createdShape = createShape(drawMode);
            if (createdShape != null) {
                createdShape.setSelected(true);
                createdShape.addPoint(latLng);
                shapes.add(createdShape);
            }
        }
        view.updateMenu();
        updateSources();
    }

    @Nullable
    private Shape createShape(DrawMode drawMode) {
        String featureId = UUID.randomUUID().toString();
        ArrayList<ShapePoint> points = new ArrayList<>();
        Shape createdShape = null;
        switch (drawMode) {
            case POINT:
               createdShape = new PointShape(featureId, points);
                break;
            case LINE:
                createdShape = new LineShape(featureId, points);
                break;
            case AREA:
                createdShape = new AreaShape(featureId, points);
                break;
            default:
                break;
        }
        return createdShape;
    }

    public void onNewDrawModePressed(DrawMode drawMode) {
        switch (drawMode) {
            case POINT:
                view.enablePointDrawMode();
                break;
            case LINE:
                view.enableLineDrawMode();
                break;
            case AREA:
                view.enableAreaDrawMode();
                break;
            default:
                break;
        }
        unSelectAllFeatures();
        updateSources();
    }

    public void onMapStyleUpdated() {
        view.displayNewMapStyle(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()),
                viewFeatures.getListOfCoordinates());
    }

    public void onSavePressed(boolean changed) {
        if (isValidShape() && changed) {
            String featureString = featureMapper.createFeaturesToSave(shapes);
            view.setShapeResult(featureString);
        } else {
            view.setCanceledResult();
        }
    }

    @Nullable
    private Shape getSelectedShape() {
        for (Shape shape : shapes) {
            if (shape.isSelected()) {
                return shape;
            }
        }
        return null;
    }

    //TODO: validate shapes
    public boolean isValidShape() {
        List<Feature> features = viewFeatures.getFeatures();
        return features.size() > 0;
    }

    public void onDeletePointConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shape.removeSelectedPoint();
            if (shape.getPoints().size() == 0) {
                shapes.remove(shape);
            }
            updateSources();
            view.updateMenu();
        }
    }

    public void onDeleteShapeConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shapes.remove(shape);
            updateSources();
            view.updateMenu();
        }
    }

    private void updateSources() {
        viewFeatures = featureMapper.toViewFeatures(shapes);
        view.updateSources(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()));
    }

    private Shape selectFeatureFromPoint(Feature feature) {
        String selectedFeatureId = feature.getStringProperty(GeoShapeConstants.FEATURE_ID);
        String selectedPointId = feature.getStringProperty(GeoShapeConstants.POINT_ID);
        Shape selectedShape = null;
        for (Shape shape : shapes) {
            if (shape.getFeatureId().equals(selectedFeatureId)) {
                shape.select(selectedPointId);
                selectedShape = shape;
            } else {
                shape.unSelect();
            }
        }
        return selectedShape;
    }

    private void unSelectAllFeatures() {
        for (Shape shape : shapes) {
            shape.unSelect();
        }
    }
}
