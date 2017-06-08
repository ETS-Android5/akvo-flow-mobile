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

package org.akvo.flow.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

public class BarcodeQuestionViewMultiple extends QuestionView {

    private boolean mMultiple;
    private RecyclerView responses;

    public BarcodeQuestionViewMultiple(Context context, Question q, SurveyListener surveyListener) {
        super(context, q, surveyListener);
        init();
    }

    private void init() {
        setQuestionView(R.layout.barcode_question_view_multiple);

        mMultiple = getQuestion().isAllowMultiple();

        responses = (RecyclerView)findViewById(R.id.responses_recycler_view);

    }


    @Override
    public void questionComplete(Bundle barcodeData) {
        if (barcodeData != null) {
            String value = barcodeData.getString(ConstantUtil.BARCODE_CONTENT);
//            if (mMultiple) {
//                addValue(value);
//            } else {
//                mInputText.setText(value);
//            }
            captureResponse();
        }
    }

    /**
     * restores the data and turns on the complete icon if the content is
     * non-null
     */
    @Override
    public void rehydrate(QuestionResponse resp) {
        super.rehydrate(resp);
    }

    /**
     * clears the file path and the complete icon
     */
    @Override
    public void resetQuestion(boolean fireEvent) {
        super.resetQuestion(fireEvent);
    }

    /**
     * pulls the data out of the fields and saves it as a response object,
     * possibly suppressing listeners
     */
    public void captureResponse(boolean suppressListeners) {
        StringBuilder builder = new StringBuilder();
//        if (mMultiple) {
//            for (int i = 0; i < mInputContainer.getChildCount(); i++) {
//                View v = mInputContainer.getChildAt(i);
//                String value = ((EditText) v.findViewById(R.id.input)).getText().toString();
//                if (!TextUtils.isEmpty(value)) {
//                    builder.append(value);
//                    if (i < mInputContainer.getChildCount() - 1) {
//                        builder.append("|");
//                    }
//                }
//            }
//        }
//        String value = mInputText.getText().toString();
//        if (!TextUtils.isEmpty(value)) {
//            builder.append(value);
//        }
        setResponse(new QuestionResponse(builder.toString(), ConstantUtil.VALUE_RESPONSE_TYPE,
                        getQuestion().getId()),
                suppressListeners);
    }
}
