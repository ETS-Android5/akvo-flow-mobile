/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.data.datasource.preferences;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

@Singleton
public class SharedPreferencesDataSource {

    private static final String KEY_CELL_UPLOAD = "data.cellular.upload";
    private static final boolean DEFAULT_VALUE_CELL_UPLOAD = false;

    private final SharedPreferences preferences;

    @Inject
    public SharedPreferencesDataSource(SharedPreferences prefs) {
        this.preferences = prefs;
    }

    public Observable<Boolean> mobileSyncEnabled() {
        return Observable.just(getBoolean(KEY_CELL_UPLOAD, DEFAULT_VALUE_CELL_UPLOAD));
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }
}