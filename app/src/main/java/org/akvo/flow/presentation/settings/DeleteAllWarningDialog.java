/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.akvo.flow.R;

public class DeleteAllWarningDialog extends DialogFragment {

    public static final String TAG = "DeleteAllWarning";
    private static final String PARAM_UNSENT_DATA = "unsent_data";

    private int messageId;
    private DeleteAllListener listener;

    public DeleteAllWarningDialog() {
    }

    public static DeleteAllWarningDialog newInstance(boolean unsentDataExists) {
        Bundle arguments = new Bundle(1);
        arguments.putBoolean(PARAM_UNSENT_DATA, unsentDataExists);
        DeleteAllWarningDialog warningDialog = new DeleteAllWarningDialog();
        warningDialog.setArguments(arguments);
        return warningDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof DeleteAllListener) {
            listener = (DeleteAllListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement DeleteAllListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean unsentDataExists = getArguments().getBoolean(PARAM_UNSENT_DATA);
        messageId = unsentDataExists ? R.string.unsentdatawarning : R.string.deletealldatawarning;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(messageId)
                .setCancelable(true)
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null) {
                            listener.deleteAllConfirmed();
                        }
                    }
                })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    public interface DeleteAllListener {

        void deleteAllConfirmed();
    }
}
