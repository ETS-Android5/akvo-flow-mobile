/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.ResponseColumns;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.ui.view.QuestionListView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.ViewUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyActivity extends ActionBarActivity implements TabListener,
        QuestionInteractionListener, QuestionListView.OnFragmentInteractionListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    private static final int PHOTO_ACTIVITY_REQUEST = 1;
    private static final int VIDEO_ACTIVITY_REQUEST = 2;
    private static final int SCAN_ACTIVITY_REQUEST  = 3;

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    /**
     * When a request is done to perform photo, video, barcode scan, etc
     * we store the question id, so we can notify later the status of such
     * operation.
     * TODO: Design how to notify the result back. Broadcast notification?
     */
    private String mRequestQuestionId;

    private ViewPager mPager;
    private TabsAdapter mAdapter;

    private boolean mReadOnly;
    private long mSurveyInstanceId;// TODO: Load/Create survey instance
    private long mUserId;
    private String mRecordId;
    private SurveyGroup mSurveyGroup;
    private Survey mSurvey;
    private SurveyDbAdapter mDatabase;

    private String[] mLanguages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        // Load survey
        final String surveyId = getIntent().getStringExtra(ConstantUtil.SURVEY_ID_KEY);
        loadSurvey(surveyId);
        loadLanguages();

        if (mSurvey == null) {
            Log.e(TAG, "mSurvey is null. Finishing the Activity...");
            finish();
        }

        mReadOnly = getIntent().getBooleanExtra(ConstantUtil.READONLY_KEY, false);
        mUserId = getIntent().getLongExtra(ConstantUtil.USER_ID_KEY, 0);
        mSurveyInstanceId = getIntent().getLongExtra(ConstantUtil.RESPONDENT_ID_KEY, 0);
        mSurveyGroup = (SurveyGroup)getIntent().getSerializableExtra(ConstantUtil.SURVEY_GROUP);
        mRecordId = getIntent().getStringExtra(ConstantUtil.SURVEYED_LOCALE_ID);

        if (mSurveyInstanceId == 0) {
            // If no survey instance is passed in, we need to create one
            // TODO: Ensure is not recreated upon rotation.
            mSurveyInstanceId = mDatabase.createOrLoadSurveyRespondent(surveyId,
                    String.valueOf(mUserId), mSurveyGroup.getId(), mRecordId);
        }

        // Set the survey name as Activity title
        setTitle(mSurvey.getName());
        mPager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new TabsAdapter();
        mAdapter.load();// Instantiate tabs. TODO: Consider doing this op. in a background thread.
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(mAdapter);

        setupActionBar();
        loadState(false);// TODO: Implement prefill functionality
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            // load from file
            in = FileUtil.getFileInputStream(surveyMeta.getFileName(),
                    ConstantUtil.DATA_DIR, false, this);
            mSurvey = SurveyDao.loadSurvey(surveyMeta, in);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
    }

    private void loadState(boolean prefill) {
        Map<String, QuestionResponse> responses = new HashMap<String, QuestionResponse>();

        Cursor cursor = mDatabase.getResponses(mSurveyInstanceId);

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(ResponseColumns._ID);
            int answerCol = cursor.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            int typeCol = cursor.getColumnIndexOrThrow(ResponseColumns.TYPE);
            int qidCol = cursor.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
            int includeCol = cursor.getColumnIndexOrThrow(ResponseColumns.INCLUDE);
            int scoreCol = cursor.getColumnIndexOrThrow(ResponseColumns.SCORED_VAL);
            int strengthCol = cursor.getColumnIndexOrThrow(ResponseColumns.STRENGTH);

            if (cursor.moveToFirst()) {
                do {
                    QuestionResponse response = new QuestionResponse();
                    response.setId(cursor.getLong(idCol));
                    response.setRespondentId(mSurveyInstanceId);// No need to read the cursor
                    response.setValue(cursor.getString(answerCol));
                    response.setType(cursor.getString(typeCol));
                    response.setQuestionId(cursor.getString(qidCol));
                    response.setIncludeFlag(cursor.getInt(includeCol) == 1);
                    response.setScoredValue(cursor.getString(scoreCol));
                    response.setStrength(cursor.getString(strengthCol));

                    responses.put(response.getQuestionId(), response);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        mAdapter.loadState(responses, prefill);
    }

    private void saveState() {
        mAdapter.saveState(mSurveyInstanceId);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        for (QuestionGroup group : mSurvey.getQuestionGroups()) {
            actionBar.addTab(actionBar.newTab()
                    .setText(group.getHeading())
                    .setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.records_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isReadOnly() {
        return mReadOnly;
    }

    @Override
    public String getSurveyId() {
        return mSurvey.getId();
    }

    @Override
    public long getSurveyInstanceId () {
        return mSurveyInstanceId;
    }

    @Override
    public String getDefaultLang() {
        String lang = mSurvey.getLanguage();
        if (TextUtils.isEmpty(lang)) {
            lang = ConstantUtil.ENGLISH_CODE;
        }
        return lang;
    }

    @Override
    public String[] getLanguages() {
        return mLanguages;
    }

    private void loadLanguages() {
        String langsSelection = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY);
        LangsPreferenceData langsPrefData = LangsPreferenceUtil.createLangPrefData(this,
                langsSelection, langsPresentIndexes);
        mLanguages = LangsPreferenceUtil.getSelectedLangCodes(this,
                langsPrefData.getLangsSelectedMasterIndexArray(),
                langsPrefData.getLangsSelectedBooleanArray(),
                R.array.alllanguagecodes);
    }

    /**
     * event handler that can be used to handle events fired by individual
     * questions at the Activity level. Because we can't launch the photo
     * activity from a view (we need to launch it from the activity), the photo
     * question view fires a QuestionInteractionEvent (to which this activity
     * listens). When we get the event, we can then spawn the camera activity.
     * Currently, this method supports handing TAKE_PHOTO_EVENT and
     * VIDEO_TIP_EVENT types
     */
    public void onQuestionInteraction(QuestionInteractionEvent event) {
        if (QuestionInteractionEvent.TAKE_PHOTO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_PHOTO_NAME_PREFIX + IMAGE_SUFFIX)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, PHOTO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, VIDEO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.SCAN_BARCODE_EVENT.equals(event.getEventType())) {
            Intent intent = new Intent(ConstantUtil.BARCODE_SCAN_INTENT);
            try {
                startActivityForResult(intent, SCAN_ACTIVITY_REQUEST);
                if (event.getSource() != null) {
                    mRequestQuestionId = event.getSource().getQuestion().getId();
                } else {
                    Log.e(TAG, "Question source was null in the event");
                }
            } catch (ActivityNotFoundException ex) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.barcodeerror);
                builder.setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            }
        } else if (QuestionInteractionEvent.QUESTION_CLEAR_EVENT.equals(event.getEventType())) {
            mDatabase.deleteResponse(mSurveyInstanceId, event.getSource().getQuestion().getId());
        }
    }

    /*
     * Check SD card space. Warn by dialog popup if it is getting low. Return to
     * home screen if completely full.
     */
    public void spaceLeftOnCard() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // TODO: more specific warning if card not mounted?
        }
        // compute space left
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double) stat.getAvailableBlocks()
                * (double) stat.getBlockSize();
        // One binary gigabyte equals 1,073,741,824 bytes.
        // double gigaAvailable = sdAvailSize / 1073741824;
        // One binary megabyte equals 1 048 576 bytes.
        long megaAvailable = (long) Math.floor(sdAvailSize / 1048576.0);

        // keep track of changes
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // assume we had space before
        long lastMegaAvailable = settings.getLong("cardMBAvaliable", 101L);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("cardMBAvaliable", megaAvailable);
        // Commit the edits!
        editor.commit();

        if (megaAvailable <= 0L) {// All out, OR media not mounted
            // Bounce user
            ViewUtil.showConfirmDialog(R.string.nocardspacetitle,
                    R.string.nocardspacedialog, this, false,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                            finish();
                        }
                    }
            );
            return;
        }

        // just issue a warning if we just descended to or past a number on the list
        if (megaAvailable < lastMegaAvailable) {
            for (long l = megaAvailable; l < lastMegaAvailable; l++) {
                if (ConstantUtil.SPACE_WARNING_MB_LEVELS.contains(Long.toString(l))) {
                    // display how much space is left
                    String s = getResources().getString(R.string.lowcardspacedialog);
                    s = s.replace("%%%", Long.toString(megaAvailable));
                    ViewUtil.showConfirmDialog(
                            R.string.lowcardspacetitle,
                            s,
                            this,
                            false,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (dialog != null) {
                                        dialog.dismiss();
                                    }
                                }
                            },
                            null);
                    return; // only one warning per survey, even of we passed >1 limit
                }
            }
        }
    }

    /**
     * sets up question dependencies across question groups and registers
     * questionInteractionListeners on the dependent views. This should be
     * called each time a new tab is hydrated. It will iterate over all
     * questions in the survey and install dependencies and the
     * questionInteractionListeners. After installation, it will check to see if
     * the parent question contains a response. If so, it will fire a
     * questionInteractionEvent to ensure dependent questions are put into the
     * correct state
     *
     * @param group
     */
    public void establishDependencies(QuestionGroup group) {
        // TODO
    }

    class TabsAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
        private List<QuestionGroup> mQuestionGroups;
        private List<QuestionListView> mQuestionListViews;
        
        public TabsAdapter() {
            mQuestionGroups = mSurvey.getQuestionGroups();
            mQuestionListViews = new ArrayList<QuestionListView>();
        }

        public void load() {
            for (QuestionGroup group : mQuestionGroups) {
                QuestionListView questionListView = new QuestionListView(SurveyActivity.this,
                        SurveyActivity.this, group, mDatabase);

                mQuestionListViews.add(questionListView);
            }
        }

        public void loadState(Map<String, QuestionResponse> responses, boolean prefill) {
            for (QuestionListView questionListView : mQuestionListViews) {
                questionListView.loadState(responses, prefill);
            }
        }

        public void saveState(long surveyInstanceId) {
            for (QuestionListView questionListView : mQuestionListViews) {
                questionListView.saveState(surveyInstanceId);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mQuestionListViews.get(position);// Already instantiated

            container.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object view) {
            container.removeView((QuestionListView) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return mQuestionGroups.size();
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return mQuestionGroups.get(position).getHeading();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Select the corresponding tab
            getSupportActionBar().setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }
    
}
