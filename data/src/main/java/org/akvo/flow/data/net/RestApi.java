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

package org.akvo.flow.data.net;

import android.text.TextUtils;

import org.akvo.flow.data.entity.ApiApkData;
import org.akvo.flow.data.entity.ApiFilesResult;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.entity.Transmission;
import org.akvo.flow.data.net.gae.DataPointDownloadService;
import org.akvo.flow.data.net.gae.DeviceFilesService;
import org.akvo.flow.data.net.gae.FlowApiService;
import org.akvo.flow.data.net.gae.ProcessingNotificationService;
import org.akvo.flow.data.net.s3.AmazonAuthHelper;
import org.akvo.flow.data.net.s3.AwsS3;
import org.akvo.flow.data.net.s3.BodyCreator;
import org.akvo.flow.data.util.ApiUrls;
import org.akvo.flow.domain.util.DeviceHelper;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Singleton;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

@Singleton
public class RestApi {
    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_PUT_PRIVATE = "PUT\n%s\n%s\n%s\n/%s/%s";// md5, type, date, bucket, obj
    private static final String PAYLOAD_GET = "GET\n\n\n%s\n/%s/%s";// date, bucket, obj
    private static final String SURVEYS_FOLDER = "surveys";

    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;
    private final String version;
    private final ApiUrls apiUrls;
    private final AmazonAuthHelper amazonAuthHelper;
    private final DateFormat dateFormat;
    private final BodyCreator bodyCreator;

    public RestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory, String version,
            ApiUrls apiUrls, AmazonAuthHelper amazonAuthHelper, DateFormat dateFormat,
            BodyCreator bodyCreator) {
        this.androidId = deviceHelper.getAndroidId();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
        this.version = version;
        this.apiUrls = apiUrls;
        this.amazonAuthHelper = amazonAuthHelper;
        this.dateFormat = dateFormat;
        this.bodyCreator = bodyCreator;
    }

    @SuppressWarnings("unchecked")
    public Single<ApiLocaleResult> downloadDataPoints(long surveyId) {
        return serviceFactory.createRetrofitServiceWithInterceptor(DataPointDownloadService.class,
                apiUrls.getGaeUrl()).getAssignedDataPoints(androidId, surveyId + "")
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading datapoints for survey: " + surveyId));
    }

    @SuppressWarnings("unchecked")
    public Observable<ApiFilesResult> getPendingFiles(List<String> formIds, String deviceId) {
        return serviceFactory.createRetrofitService(DeviceFilesService.class, apiUrls.getGaeUrl())
                .getFilesLists(phoneNumber, androidId, imei, version, deviceId, formIds)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error getting device pending files"));
    }

    @SuppressWarnings("unchecked")
    public Observable<?> notifyFileAvailable(String action, String formId,
            String filename, String deviceId) {
        return serviceFactory
                .createRetrofitService(ProcessingNotificationService.class, apiUrls.getGaeUrl())
                .notifyFileAvailable(action, formId, filename, phoneNumber, androidId, imei,
                        version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error notifying the file is available"));
    }

    public Observable<Response<ResponseBody>> uploadFile(Transmission transmission) {
        S3File s3File = transmission.getS3File();
        final String date = getDate();

        if (s3File.isPublic()) {
            return uploadPublicFile(date, s3File);
        } else {
            return uploadPrivateFile(date, s3File);
        }
    }

    @SuppressWarnings("unchecked")
    public Observable<ApiApkData> loadApkData(String appVersion) {
        return serviceFactory.createRetrofitService(FlowApiService.class, apiUrls.getGaeUrl())
                .loadApkData(appVersion)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading apk data for version " + appVersion));
    }

    @SuppressWarnings("unchecked")
    public Observable<String> downloadFormHeader(String formId, String deviceId) {
        return serviceFactory
                .createScalarsRetrofitService(FlowApiService.class, apiUrls.getGaeUrl())
                .downloadFormHeader(formId, phoneNumber, androidId, imei, version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading form " + formId + " header"));
    }

    @SuppressWarnings("unchecked")
    public Observable<String> downloadFormsHeader(String deviceId) {
        return serviceFactory
                .createScalarsRetrofitService(FlowApiService.class, apiUrls.getGaeUrl())
                .downloadFormsHeader(phoneNumber, androidId, imei, version, deviceId)
                .onErrorResumeNext(new ErrorLoggerFunction("Error downloading all form headers"));
    }

    @SuppressWarnings("unchecked")
    public Observable<ResponseBody> downloadArchive(final String fileName) {
        final String date = getDate();
        String authorization = amazonAuthHelper
                .getAmazonAuthForGet(date, PAYLOAD_GET, SURVEYS_FOLDER + "/" + fileName);
        return createRetrofitService().getSurvey(SURVEYS_FOLDER, fileName, date, authorization)
                .onErrorResumeNext(new ErrorLoggerFunction(
                        "Error downloading " + fileName + " from s3"));
    }

    private Observable<Response<ResponseBody>> uploadPublicFile(String date, final S3File s3File) {
        String authorization = amazonAuthHelper
                .getAmazonAuthForPut(date, PAYLOAD_PUT_PUBLIC, s3File);
        return createRetrofitService()
                .uploadPublic(s3File.getDir(), s3File.getFilename(), s3File.getMd5Base64(),
                        s3File.getContentType(), date, authorization,
                        bodyCreator.createBody(s3File))
                .concatMap(
                        new Function<Response<ResponseBody>, Observable<Response<ResponseBody>>>() {
                            @Override
                            public Observable<Response<ResponseBody>> apply(
                                    Response<ResponseBody> response) {
                                return verifyResponse(response, s3File);
                            }
                        });
    }

    private AwsS3 createRetrofitService() {
        return serviceFactory.createRetrofitService(AwsS3.class, apiUrls.getS3Url());
    }

    private Observable<Response<ResponseBody>> uploadPrivateFile(String date, final S3File s3File) {
        String authorization = amazonAuthHelper
                .getAmazonAuthForPut(date, PAYLOAD_PUT_PRIVATE, s3File);
        RequestBody body = bodyCreator.createBody(s3File);
        return createRetrofitService()
                .upload(s3File.getDir(), s3File.getFilename(), s3File.getMd5Base64(),
                        s3File.getContentType(), date, authorization, body)
                .concatMap(
                        new Function<Response<ResponseBody>, Observable<Response<ResponseBody>>>() {
                            @Override
                            public Observable<Response<ResponseBody>> apply(
                                    Response<ResponseBody> response) {
                                return verifyResponse(response, s3File);
                            }
                        });
    }

    private Observable<Response<ResponseBody>> verifyResponse(Response<ResponseBody> response,
            S3File s3File) {
        if (response.isSuccessful()) {
            String etag = getEtag(response);
            if (TextUtils.isEmpty(etag) || !etag.equals(s3File.getMd5Hex())) {
                return Observable.error(new Exception(
                        "File upload to S3 Failed" + s3File.getFilename()));
            }
        } else {
            return Observable.error(new HttpException(response));
        }
        return Observable.just(response);
    }

    @Nullable
    private String getEtag(Response<ResponseBody> response) {
        String eTag = response.headers().get("ETag");
        if (!TextUtils.isEmpty(eTag)) {
            eTag = eTag.replaceAll("\"", "");
        }
        return eTag;
    }

    private String getDate() {
        return dateFormat.format(new Date()) + "GMT";
    }

}
