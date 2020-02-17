/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.form.view.languages

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.akvo.flow.R
import org.akvo.flow.util.ConstantUtil

class LanguagesErrorDialog : DialogFragment() {

    lateinit var formId: String

    companion object {

        const val TAG = "LanguagesErrorDialog"

        @JvmStatic
        fun newInstance(formId: String): LanguagesErrorDialog {
            return LanguagesErrorDialog().apply {
                arguments = Bundle().apply {
                    putString(ConstantUtil.FORM_ID_EXTRA, formId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        formId = arguments!!.getString(ConstantUtil.FORM_ID_EXTRA, "")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
            .setTitle(R.string.langmandatorytitle)
            .setMessage(R.string.langmandatorytext)
            .setPositiveButton(R.string.okbutton) { dialog, _ ->
                dialog?.dismiss()
                activity?.let {
                    LanguagesDialogFragment.newInstance(formId)
                        .show(it.supportFragmentManager, LanguagesDialogFragment.TAG)
                }
            }
            .create()
    }
}