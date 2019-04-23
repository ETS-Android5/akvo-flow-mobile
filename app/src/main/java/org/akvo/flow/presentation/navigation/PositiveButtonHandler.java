/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.navigation;

import android.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.widget.Button;

class PositiveButtonHandler {

    @NonNull
    private final DialogFragment dialogFragment;

    PositiveButtonHandler(@NonNull DialogFragment dialogFragment) {
        this.dialogFragment = dialogFragment;
    }

    void disablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(false);
    }

    void enablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(true);
    }

    private Button getPositiveButton() {
        AlertDialog dialog = (AlertDialog) dialogFragment.getDialog();
        return dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }
}
