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

package org.akvo.flow.util.files;

import android.content.Context;
import android.support.annotation.NonNull;

import org.akvo.flow.util.files.FileBrowser;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

public class FormFileBrowser {

    private static final String DIR_FORMS = "forms";

    private final FileBrowser fileBrowser;

    @Inject
    public FormFileBrowser(FileBrowser fileBrowser) {
        this.fileBrowser = fileBrowser;
    }

    @NonNull
    public File getExistingAppInternalFolder(Context context) {
        return fileBrowser.getExistingAppInternalFolder(context, DIR_FORMS);
    }

    @NonNull
    public File findFile(Context context, String fileName) {
        return fileBrowser.findFile(context, DIR_FORMS, fileName);
    }

    @NonNull
    public List<File> findAllPossibleFolders(Context context) {
        return fileBrowser.findAllPossibleFolders(context, DIR_FORMS);
    }
}
