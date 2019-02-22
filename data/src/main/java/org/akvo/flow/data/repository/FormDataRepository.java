/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.repository;

import android.database.Cursor;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.ApiFormHeader;
import org.akvo.flow.data.entity.FormHeaderParser;
import org.akvo.flow.data.entity.FormIdMapper;
import org.akvo.flow.data.entity.XmlParser;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.data.util.FlowFileBrowser;
import org.akvo.flow.domain.repository.FormRepository;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class FormDataRepository implements FormRepository {

    private static final String TEST_FORM_ID = "0";

    private final FormHeaderParser formHeaderParser;
    private final XmlParser xmlParser;
    private final RestApi restApi;
    private final DataSourceFactory dataSourceFactory;
    private final FormIdMapper formIdMapper;

    @Inject
    public FormDataRepository(FormHeaderParser formHeaderParser, XmlParser xmlParser,
            RestApi restApi, DataSourceFactory dataSourceFactory, FormIdMapper formIdMapper) {
        this.formHeaderParser = formHeaderParser;
        this.xmlParser = xmlParser;
        this.restApi = restApi;
        this.dataSourceFactory = dataSourceFactory;
        this.formIdMapper = formIdMapper;
    }

    @Override
    public Observable<Boolean> loadForm(String formId, String deviceId) {
        final DatabaseDataSource dataBaseDataSource = dataSourceFactory.getDataBaseDataSource();
        if (TEST_FORM_ID.equals(formId)) {
            return dataBaseDataSource.installTestForm();
        } else {
            return downloadFormHeader(formId, deviceId);
        }
    }

    @Override
    public Observable<Integer> reloadForms(final String deviceId) {
        final DatabaseDataSource dataBaseDataSource = dataSourceFactory.getDataBaseDataSource();
        return dataBaseDataSource.getFormIds()
                .map(new Function<Cursor, List<String>>() {
                    @Override
                    public List<String> apply(Cursor cursor) {
                        return formIdMapper.mapToFormId(cursor);
                    }
                })
                .concatMap(new Function<List<String>, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(final List<String> formIds) {
                        return dataBaseDataSource.deleteAllForms()
                                .concatMap(new Function<Boolean, Observable<Integer>>() {
                                    @Override
                                    public Observable<Integer> apply(Boolean aBoolean) {
                                        return downloadFormHeaders(formIds, deviceId);
                                    }
                                });
                    }
                });
    }

    @Override
    public Observable<Integer> downloadForms(final String deviceId) {
        return restApi.downloadFormsHeader(deviceId)
                .map(new Function<String, List<ApiFormHeader>>() {
                    @Override
                    public List<ApiFormHeader> apply(String response) {
                        return formHeaderParser.parseMultiple(response);
                    }
                })
                .concatMap(new Function<List<ApiFormHeader>, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(List<ApiFormHeader> apiFormHeaders) {
                        return downloadForms(apiFormHeaders);
                    }
                });
    }

    private Observable<Boolean> downloadFormHeader(String formId, String deviceId) {
        return restApi.downloadFormHeader(formId, deviceId)
                .map(new Function<String, ApiFormHeader>() {
                    @Override
                    public ApiFormHeader apply(String response) {
                        return formHeaderParser.parseOne(response);
                    }
                })
                .concatMap(new Function<ApiFormHeader, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(final ApiFormHeader apiFormHeader) {
                        return insertAndDownload(apiFormHeader);
                    }
                });
    }

    private Observable<Integer> downloadForms(List<ApiFormHeader> apiFormHeaders) {
        return Observable.fromIterable(apiFormHeaders)
                .concatMap(new Function<ApiFormHeader, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(final ApiFormHeader apiFormHeader) {
                        return insertAndDownload(apiFormHeader);

                    }
                })
                .toList()
                .toObservable()
                .map(new Function<List<Boolean>, Integer>() {
                    @Override
                    public Integer apply(List<Boolean> booleans) {
                        return booleans.size();
                    }
                });
    }

    private Observable<Boolean> insertAndDownload(final ApiFormHeader apiFormHeader) {
        return dataSourceFactory.getDataBaseDataSource().insertSurveyGroup(apiFormHeader)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean ignored) {
                        return downloadForm(apiFormHeader);
                    }
                });
    }

    private Observable<Integer> downloadFormHeaders(List<String> formIds, final String deviceId) {
        return Observable.fromIterable(formIds)
                .concatMap(new Function<String, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(String formId) {
                        return downloadFormHeader(formId, deviceId);
                    }
                })
                .toList()
                .toObservable()
                .map(new Function<List<Boolean>, Integer>() {
                    @Override
                    public Integer apply(List<Boolean> booleans) {
                        return booleans.size();
                    }
                });
    }

    private Observable<Boolean> downloadForm(final ApiFormHeader apiFormHeader) {
        return dataSourceFactory.getDataBaseDataSource().formNeedsUpdate(apiFormHeader)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean updateNeeded) {
                        if (updateNeeded) {
                            return downloadAndSaveForm(apiFormHeader);
                        } else {
                            return Observable.just(true);
                        }
                    }

                });
    }

    private Observable<Boolean> downloadAndSaveForm(final ApiFormHeader apiFormHeader) {
        return downloadAndExtractFile(apiFormHeader.getId() + FlowFileBrowser.ZIP_SUFFIX,
                FlowFileBrowser.DIR_FORMS)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean aBoolean) {
                        return saveForm(apiFormHeader);
                    }
                });
    }

    private Observable<Boolean> downloadAndExtractFile(String fileName, final String folder) {
        return restApi.downloadArchive(fileName)
                .concatMap(new Function<ResponseBody, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(ResponseBody responseBody) {
                        return dataSourceFactory.getFileDataSource()
                                .extractRemoteArchive(responseBody, folder);

                    }
                });
    }

    private Observable<Boolean> saveForm(final ApiFormHeader apiFormHeader) {
        return dataSourceFactory.getFileDataSource().getFormFile(apiFormHeader.getId())
                .map(new Function<InputStream, List<String>>() {
                    @Override
                    public List<String> apply(InputStream inputStream) {
                        return xmlParser.parse(inputStream);
                    }
                })
                .concatMap(new Function<List<String>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(List<String> resources) {
                        return downloadResources(resources)
                                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> apply(Boolean aBoolean) {
                                        return dataSourceFactory.getDataBaseDataSource()
                                                .insertSurvey(apiFormHeader, true);
                                    }
                                })
                                .doOnError(new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) {
                                        Timber.e(throwable);
                                        dataSourceFactory.getDataBaseDataSource()
                                                .insertSurvey(apiFormHeader, false);
                                    }
                                });
                    }
                });

    }

    private Observable<Boolean> downloadResources(List<String> resources) {
        return Observable.fromIterable(resources)
                .concatMap(new Function<String, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(String resource) {
                        return downloadAndExtractFile(resource + FlowFileBrowser.ZIP_SUFFIX,
                                FlowFileBrowser.DIR_RES);
                    }
                })
                .toList()
                .toObservable()
                .map(new Function<List<Boolean>, Boolean>() {
                    @Override
                    public Boolean apply(List<Boolean> ignored) {
                        return true;
                    }
                });
    }
}
