/*
 * Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.dao.SurveyDao;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.SurveyLanguagesDataSource;
import org.akvo.flow.database.SurveyLanguagesDbDataSource;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.form.FormPresenter;
import org.akvo.flow.presentation.form.FormView;
import org.akvo.flow.presentation.form.mobiledata.MobileDataSettingDialog;
import org.akvo.flow.service.DataPointUploadWorker;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.LanguageAdapter;
import org.akvo.flow.ui.adapter.SurveyTabAdapter;
import org.akvo.flow.ui.model.Language;
import org.akvo.flow.ui.model.LanguageMapper;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.geolocation.GeoFieldsResetConfirmDialogFragment;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StorageHelper;
import org.akvo.flow.util.ViewUtil;
import org.akvo.flow.util.files.FormFileBrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

import static org.akvo.flow.util.ViewUtil.showConfirmDialog;

public class FormActivity extends BackActivity implements SurveyListener,
        QuestionInteractionListener, FormView,
        GeoFieldsResetConfirmDialogFragment.GeoFieldsResetConfirmListener,
        MobileDataSettingDialog.MobileDataSettingListener {

    @Inject
    FormFileBrowser formFileBrowser;

    @Inject
    MediaFileHelper mediaFileHelper;

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Prefs prefs;

    @Inject
    FormPresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    private final Navigator navigator = new Navigator();
    private final StorageHelper storageHelper = new StorageHelper();

    /**
     * When a request is done to perform photo, video, barcode scan, etc we store
     * the question id, so we can notify later the result of such operation.
     */
    private String mRequestQuestionId;

    private ViewPager mPager;
    private SurveyTabAdapter mAdapter;
    private ProgressBar progressBar;
    private View rootView;

    /**
     * flag to represent whether the Survey can be edited or not
     */
    private boolean mReadOnly;
    private long mSurveyInstanceId;
    private long mSessionStartTime;
    private String mRecordId;
    private SurveyGroup mSurveyGroup;
    private Survey mSurvey;

    private SurveyLanguagesDataSource surveyLanguagesDataSource;

    private String[] mLanguages;
    private LanguageMapper languageMapper;

    private Map<String, QuestionResponse> mQuestionResponses; // QuestionId - QuestionResponse
    private String surveyId;

    private Uri imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_activity);
        initializeInjector();
        presenter.setView(this);
        // Read all the params. Note that the survey instance id is now mandatory
        Intent intent = getIntent();
        surveyId = intent.getStringExtra(ConstantUtil.FORM_ID_EXTRA);
        mReadOnly = intent.getBooleanExtra(ConstantUtil.READ_ONLY_EXTRA, false);
        mSurveyInstanceId = intent.getLongExtra(ConstantUtil.RESPONDENT_ID_EXTRA, 0);
        mSurveyGroup = (SurveyGroup) intent.getSerializableExtra(ConstantUtil.SURVEY_GROUP_EXTRA);
        mRecordId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);

        mQuestionResponses = new HashMap<>();
        mDatabase.open();

        Context context = getApplicationContext();
        languageMapper = new LanguageMapper(context);
        surveyLanguagesDataSource = new SurveyLanguagesDbDataSource(context);

        if (savedInstanceState != null) {
            mRequestQuestionId = savedInstanceState.getString(ConstantUtil.REQUEST_QUESTION_ID_EXTRA);
            imagePath = savedInstanceState.getParcelable(ConstantUtil.IMAGE_FILE_KEY);
        }

        //TODO: move all loading to worker thread
        loadSurvey(surveyId);
        loadLanguages();
        if (mSurvey == null) {
            Timber.e("mSurvey is null. Finishing the Activity...");
            Toast.makeText(getApplicationContext(), R.string.error_missing_form, Toast.LENGTH_LONG)
                    .show();
            finish();
        } else {
            setupToolBar();
            // Set the survey name as Activity title
            getSupportActionBar().setTitle(mSurvey.getName());
            getSupportActionBar().setSubtitle("v " + getVersion());

            mPager = findViewById(R.id.pager);
            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mPager);
            mAdapter = new SurveyTabAdapter(this, mPager, this, this);
            mPager.setAdapter(mAdapter);

            progressBar = findViewById(R.id.progressBar);
            rootView = findViewById(R.id.coordinator_layout);
            // Initialize new survey or load previous responses
            Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
            if (!responses.isEmpty()) {
                displayResponses(responses);
            }
            spaceLeftOnCard();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ConstantUtil.REQUEST_QUESTION_ID_EXTRA, mRequestQuestionId);
        outState.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imagePath);
        super.onSaveInstanceState(outState);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    protected ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    /**
     * Display prefill option dialog, if applies. This feature is only available
     * for monitored groups, when a new survey instance is created, allowing users
     * to 'clone' responses from the previous response.
     */
    private void displayPreFillDialog() {
        final Long lastSurveyInstance = mDatabase.getLastSurveyInstance(mRecordId, mSurvey.getId());
        if (lastSurveyInstance != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.prefill_title);
            builder.setMessage(R.string.prefill_text);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preFillSurvey(lastSurveyInstance);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.show();
        }
    }

    private void preFillSurvey(long preFilledSurveyInstance) {
        Map<String, QuestionResponse> responses = mDatabase
                .getResponsesForPreFilledSurvey(preFilledSurveyInstance, mSurveyInstanceId);
        displayResponses(responses);
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            File file = formFileBrowser
                    .findFile(getApplicationContext(), surveyMeta.getFileName());
            in = new FileInputStream(file);
            mSurvey = SurveyDao.loadSurvey(surveyMeta, in);
            mSurvey.setId(surveyId);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //EMPTY
                }
            }
        }
    }

    private double getVersion() {
        double version = 0.0;
        Cursor c = mDatabase.getFormInstance(mSurveyInstanceId);
        if (c.moveToFirst()) {
            version = c.getDouble(SurveyDbAdapter.FormInstanceQuery.VERSION);
        }
        c.close();

        if (version == 0.0) {
            version = mSurvey.getVersion();// Default to current value
        }

        return version;
    }

    /**
     * Load state for the current survey instance
     */
    private void loadResponses() {
        Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
        displayResponses(responses);
    }

    /**
     * Load state with the provided responses map
     */
    private void displayResponses(Map<String, QuestionResponse> responses) {
        mQuestionResponses = responses;
        mAdapter.reset();// Propagate the change
    }

    /**
     * Handle survey session duration. Only 'active' survey time will be consider, that is,
     * the time range between onResume() and onPause() callbacks. Survey submission will also
     * stop the recording. This feature is only used if the mReadOnly flag is not active.
     *
     * @param start true if the call is to start recording, false to stop and save the duration.
     */
    private void recordDuration(boolean start) {
        if (mReadOnly) {
            return;
        }

        final long time = System.currentTimeMillis();

        if (start) {
            mSessionStartTime = time;
        } else {
            mDatabase.addSurveyDuration(mSurveyInstanceId, time - mSessionStartTime);
            // Restart the current session timer, in case we receive subsequent calls
            // to record the time, w/o setting up the timer first.
            mSessionStartTime = time;
        }
    }

    private void saveState() {
        if (!mReadOnly) {
            mDatabase.updateSurveyInstanceStatus(mSurveyInstanceId, SurveyInstanceStatus.SAVED);
            mDatabase.updateRecordModifiedDate(mRecordId, System.currentTimeMillis());

            // Record meta-data, if applies
            if (!mSurveyGroup.isMonitored() || mSurvey.getId()
                    .equals(mSurveyGroup.getRegisterSurveyId())) {
                saveRecordMetaData();
            }
        }
    }

    private void saveRecordMetaData() {
        saveRecordName();
        saveRecordLocation();
    }

    private void saveRecordLocation() {
        String localeGeoQuestion = mSurvey.getLocaleGeoQuestion();
        if (localeGeoQuestion != null) {
            QuestionResponse response = mDatabase.getResponse(mSurveyInstanceId, localeGeoQuestion);
            if (response != null) {
                mDatabase.updateSurveyedLocale(mSurveyInstanceId, response.getValue(),
                        SurveyDbAdapter.SurveyedLocaleMeta.GEOLOCATION);
            }
        }
    }

    private void saveRecordName() {
        StringBuilder builder = new StringBuilder();
        List<String> localeNameQuestions = mSurvey.getLocaleNameQuestions();

        // Check the responses given to these questions (marked as name)
        // and concatenate them so it becomes the Locale name.
        if (!localeNameQuestions.isEmpty()) {
            boolean first = true;
            for (String questionId : localeNameQuestions) {
                QuestionResponse questionResponse = mDatabase
                        .getResponse(mSurveyInstanceId, questionId);
                String answer =
                        questionResponse != null ? questionResponse.getDatapointNameValue() : null;

                if (!TextUtils.isEmpty(answer)) {
                    if (!first) {
                        builder.append(" - ");
                    }
                    builder.append(answer);
                    first = false;
                }
            }
            // Make sure the value is not larger than 500 chars
            builder.setLength(Math.min(builder.length(), 500));
            mDatabase.updateSurveyedLocale(mSurveyInstanceId, builder.toString(),
                    SurveyDbAdapter.SurveyedLocaleMeta.NAME);
        }
    }

    private void resetRecordName() {
        if (!mSurveyGroup.isMonitored() || isRegistrationForm()) {
            mDatabase.clearSurveyedLocaleName(mSurveyInstanceId);
        }
    }

    private boolean isRegistrationForm() {
        Survey registrationForm = mDatabase.getRegistrationForm(mSurveyGroup);
        return registrationForm != null && registrationForm.getId() != null && registrationForm
                .getId().equals(surveyId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.onResume();
        recordDuration(true);// Keep track of this session's duration.
        mPager.setKeepScreenOn(
                prefs.getBoolean(Prefs.KEY_SCREEN_ON, Prefs.DEFAULT_VALUE_SCREEN_ON));
    }

    @Override
    public void onPause() {
        super.onPause();
        mPager.setKeepScreenOn(false);
        mAdapter.onPause();
        recordDuration(false);
        saveState();
    }

    @Override
    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
        if (mDatabase != null) {
            mDatabase.close();
        }
        presenter.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.form_activity, menu);
        SubMenu subMenu = menu.findItem(R.id.more_submenu).getSubMenu();
        if (isReadOnly()) {
            subMenu.removeItem(R.id.clear);
            subMenu.removeItem(R.id.prefill);
        } else {
            subMenu.removeItem(R.id.view_map);
            subMenu.removeItem(R.id.transmission);
            if (!mSurveyGroup.isMonitored() ||
                    mDatabase.getLastSurveyInstance(mRecordId, mSurvey.getId()) == null) {
                subMenu.removeItem(R.id.prefill);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_lang:
                displayLanguagesDialog();
                return true;
            case R.id.clear:
                clearSurvey();
                return true;
            case R.id.prefill:
                displayPreFillDialog();
                return true;
            case R.id.view_map:
                navigator.navigateToMapActivity(this, mRecordId);
                return true;
            case R.id.transmission:
                navigator.navigateToTransmissionActivity(this, mSurveyInstanceId);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearSurvey() {
        showConfirmDialog(R.string.cleartitle, R.string.cleardesc, this, true,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabase.deleteResponses(String.valueOf(mSurveyInstanceId));
                        resetRecordName();
                        loadResponses();
                        spaceLeftOnCard();
                    }
                });
    }

    private void displayLanguagesDialog() {
        final ListView listView = createLanguagesList();
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.surveylanglabel)
                .setView(listView)
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        useSelectedLanguages((LanguageAdapter) listView.getAdapter());
                    }
                }).create();
        alertDialog.show();
    }

    private void useSelectedLanguages(LanguageAdapter languageAdapter) {
        Set<String> selectedLanguages = languageAdapter.getSelectedLanguages();
        if (selectedLanguages != null && selectedLanguages.size() > 0) {
            saveLanguages(selectedLanguages);
        } else {
            displayError();
        }
    }

    private void displayError() {
        ViewUtil.showConfirmDialog(R.string.langmandatorytitle,
                R.string.langmandatorytext, this, false,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        displayLanguagesDialog();
                    }
                });
    }

    private void saveLanguages(Set<String> selectedLanguages) {
        surveyLanguagesDataSource.saveLanguagePreferences(mSurveyGroup.getId(),
                selectedLanguages);
        loadLanguages();
        mAdapter.notifyOptionsChanged();
    }

    @NonNull
    private ListView createLanguagesList() {
        List<Language> languages = languageMapper
                .transform(mLanguages, mSurvey.getAvailableLanguageCodes());
        final LanguageAdapter languageAdapter = new LanguageAdapter(this, languages);

        final ListView listView = (ListView) LayoutInflater.from(this)
                .inflate(R.layout.languages_list, null);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(languageAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                languageAdapter.updateSelected(position);
            }
        });
        return listView;
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, final Intent intent) {
        if (mRequestQuestionId == null || resultCode != RESULT_OK) {
            mRequestQuestionId = null;
            return;
        }

        if (mAdapter.getQuestionView(mRequestQuestionId) == null) {
            // Set the result only after the QuestionView is loaded
            mAdapter.setOnTabLoadedListener(new SurveyTabAdapter.OnTabLoadedListener() {
                @Override
                public void onTabLoaded() {
                    setResult(requestCode, intent, mRequestQuestionId);
                    mAdapter.setOnTabLoadedListener(null);
                }
            });
        } else {
            setResult(requestCode, intent, mRequestQuestionId);
        }
    }

    private void setResult(int requestCode, Intent intent, String requestQuestionId) {
        switch (requestCode) {
            case ConstantUtil.PHOTO_ACTIVITY_REQUEST:
                onImageTaken();
                break;
            case ConstantUtil.VIDEO_ACTIVITY_REQUEST:
                onVideoTaken(intent.getData());
                break;
            case ConstantUtil.GET_PHOTO_ACTIVITY_REQUEST:
                onImageAcquired(intent.getData());
                break;
            case ConstantUtil.GET_VIDEO_ACTIVITY_REQUEST:
                onVideoAcquired(intent.getData());
                break;
            case ConstantUtil.CADDISFLY_REQUEST:
            case ConstantUtil.SCAN_ACTIVITY_REQUEST:
            case ConstantUtil.PLOTTING_REQUEST:
            case ConstantUtil.SIGNATURE_REQUEST:
            default:
                mAdapter.onQuestionResultReceived(requestQuestionId, intent.getExtras());
                break;
        }
        mRequestQuestionId = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (mRequestQuestionId == null) {
            return;
        }
        mAdapter.onRequestPermissionsResult(requestCode, mRequestQuestionId, permissions,
                grantResults);
        mRequestQuestionId = null;
    }

    public void requestPermissions(@NonNull String[] permissions, int requestCode,
            String questionId) {
        mRequestQuestionId = questionId;
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    private void onVideoAcquired(Uri uri) {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.VIDEO_FILE_KEY, uri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onImageAcquired(Uri imageUri) {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imageUri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onImageTaken() {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imagePath);
        mediaData.putBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, true);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onVideoTaken(Uri uri) {
        Bundle mediaData = new Bundle();
        mediaData.putBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, true);
        mediaData.putParcelable(ConstantUtil.VIDEO_FILE_KEY, uri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    @NonNull
    private String getDefaultLang() {
        //TODO: check if survey is null?
        return mSurvey.getDefaultLanguageCode();
    }

    //TODO: use loader
    private void loadLanguages() {
        Set<String> languagePreferences = surveyLanguagesDataSource
                .getLanguagePreferences(mSurveyGroup.getId());
        mLanguages = languagePreferences.toArray(new String[0]);
    }

    @Override
    public List<QuestionGroup> getQuestionGroups() {
        return mSurvey.getQuestionGroups();
    }

    @Override
    public String getDefaultLanguage() {
        return getDefaultLang();
    }

    @Override
    public String[] getLanguages() {
        return mLanguages;
    }

    @Override
    public boolean isReadOnly() {
        return mReadOnly;
    }

    @Override
    public void onSurveySubmit() {
        recordDuration(false);
        saveState();
        presenter.onSubmitPressed(mSurveyInstanceId);
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void startSync(boolean isMobileSyncAllowed) {
        DataPointUploadWorker.scheduleUpload(getApplicationContext(), isMobileSyncAllowed);
    }

    @Override
    public void dismiss() {
        mReadOnly = true;
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void showErrorExport() {
        snackBarManager.displaySnackBar(rootView, R.string.form_submit_error, this);
    }

    @Override
    public void showMobileUploadSetting(long surveyInstanceId) {
        DialogFragment fragment = MobileDataSettingDialog.newInstance(surveyInstanceId);
        fragment.show(getSupportFragmentManager(), MobileDataSettingDialog.TAG);
    }

    @Override
    public void onMobileUploadSet(long instanceId) {
        presenter.onSubmitPressed(instanceId);
    }

    @Override
    public void nextTab() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
    }

    @Override
    public void openQuestion(String questionId) {
        int tab = mAdapter.displayQuestion(questionId);
        if (tab != -1) {
            mPager.setCurrentItem(tab, true);
        }
    }

    @Override
    public Map<String, QuestionResponse> getResponses() {
        return mQuestionResponses;
    }

    @Override
    public void deleteResponse(String questionId) {
        QuestionResponse questionResponse = mQuestionResponses.remove(questionId);
        if (questionResponse != null && questionResponse.isAnswerToRepeatableGroup()) {
            mDatabase.deleteResponse(mSurveyInstanceId, questionResponse.getQuestionId(),
                    questionResponse.getIteration() + "");
        } else {
            mDatabase.deleteResponse(mSurveyInstanceId, questionId);
        }
    }

    public void deleteResponse(String questionId, String iteration) {
        mQuestionResponses.remove(questionId);
        mDatabase.deleteResponse(mSurveyInstanceId, questionId, iteration);
    }

    @Override
    public QuestionView getQuestionView(String questionId) {
        return mAdapter.getQuestionView(questionId);
    }

    @Override
    public String getDatapointId() {
        return mRecordId;
    }

    @Override
    public String getFormId() {
        return mSurvey.getId();
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
            takePhoto(event);
        } else if (QuestionInteractionEvent.GET_PHOTO_EVENT.equals(event.getEventType())) {
            navigateToGetPhoto(event);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            navigateToTakeVideo(event);
        } else if (QuestionInteractionEvent.GET_VIDEO_EVENT.equals(event.getEventType())) {
            navigateToGetVideo(event);
        } else if (QuestionInteractionEvent.SCAN_BARCODE_EVENT.equals(event.getEventType())) {
            navigateToBarcodeScanner(event);
        } else if (QuestionInteractionEvent.QUESTION_CLEAR_EVENT.equals(event.getEventType())) {
            clearQuestion(event);
        } else if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event.getEventType())) {
            storeAnswer(event);
        } else if (QuestionInteractionEvent.CADDISFLY.equals(event.getEventType())) {
            navigateToCaddisfly(event);
        } else if (QuestionInteractionEvent.PLOTTING_EVENT.equals(event.getEventType())) {
            navigateToGeoShapeActivity(event);
        } else if (QuestionInteractionEvent.ADD_SIGNATURE_EVENT.equals(event.getEventType())) {
            navigateToSignatureActivity(event);
        }
    }

    private void navigateToGetVideo(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToGetVideo(this);
    }

    private void navigateToGetPhoto(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToGetPhoto(this);
    }

    private void navigateToSignatureActivity(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToSignatureActivity(this, event.getData());
    }

    private void navigateToGeoShapeActivity(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToCreateGeoShapeActivity(this, event.getData());
    }

    private void navigateToCaddisfly(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToCaddisfly(this, event.getData(), getString(R.string.caddisfly_test));
    }

    private void storeAnswer(QuestionInteractionEvent event) {
        String questionIdKey = event.getSource().getQuestion().getId();
        QuestionResponse eventResponse = event.getSource().getResponse();

        // Store the response if it contains a value. Otherwise, delete it
        if (eventResponse != null && eventResponse.hasValue()) {
            Long id = mQuestionResponses.containsKey(questionIdKey) ?
                    mQuestionResponses.get(questionIdKey).getId() : null;
            QuestionResponse responseToSave = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(eventResponse.getValue())
                    .setType(eventResponse.getType())
                    .setId(id)
                    .setSurveyInstanceId(mSurveyInstanceId)
                    .setQuestionId(eventResponse.getQuestionId())
                    .setFilename(eventResponse.getFilename())
                    .setIncludeFlag(eventResponse.getIncludeFlag())
                    .setIteration(eventResponse.getIteration())
                    .createQuestionResponse();
            responseToSave = mDatabase.createOrUpdateSurveyResponse(responseToSave);
            mQuestionResponses.put(questionIdKey, responseToSave);
        } else {
            event.getSource().setResponse(null, true);// Invalidate previous response
            deleteResponse(questionIdKey);
        }
    }

    private void clearQuestion(QuestionInteractionEvent event) {
        String questionId = event.getSource().getQuestion().getId();
        deleteResponse(questionId);
    }

    private void navigateToBarcodeScanner(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToBarcodeScanner(this);
    }

    private void recordSourceId(QuestionInteractionEvent event) {
        if (event.getSource() != null) {
            mRequestQuestionId = event.getSource().getQuestion().getId();
        } else {
            Timber.e("Question source was null in the event");
        }
    }

    private void navigateToTakeVideo(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToTakeVideo(this);
    }

    private void takePhoto(QuestionInteractionEvent event) {
        recordSourceId(event);
        File imageTmpFile = mediaFileHelper.getTemporaryImageFile();
        if (imageTmpFile != null) {
            imagePath = FileProvider.getUriForFile(this, ConstantUtil.FILE_PROVIDER_AUTHORITY,
                    imageTmpFile);
            navigator.navigateToTakePhoto(this, imagePath);
        }
        //TODO: notify error taking pictures
    }

    /*
     * Check SD card space. Warn by dialog popup if it is getting low. Return to
     * home screen if completely full.
     */
    private void spaceLeftOnCard() {
        if (PlatformUtil.isEmulator()) {
            return;
        }
        long megaAvailable = storageHelper.getExternalStorageAvailableSpace();

        // keep track of changes
        // assume we had space before
        long lastMegaAvailable = prefs
                .getLong(Prefs.KEY_SPACE_AVAILABLE, Prefs.DEF_VALUE_SPACE_AVAILABLE);
        prefs.setLong(Prefs.KEY_SPACE_AVAILABLE, megaAvailable);

        if (megaAvailable <= 0L) {// All out, OR media not mounted
            // Bounce user
            showConfirmDialog(R.string.nocardspacetitle, R.string.nocardspacedialog, this, false,
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
                    //TODO: replace "%%%" by "%s" and use String formatting
                    String message = getResources().getString(R.string.lowcardspacedialog);
                    message = message.replace("%%%", Long.toString(megaAvailable));
                    showConfirmDialog(R.string.lowcardspacetitle, message, this, false,
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

    @Override
    public void confirmGeoFieldReset(String questionId) {
        View viewWithTag = mPager.findViewWithTag(questionId);
        if (viewWithTag instanceof GeoQuestionView) {
            ((GeoQuestionView) viewWithTag).startListeningToLocation();
        }
    }
}
