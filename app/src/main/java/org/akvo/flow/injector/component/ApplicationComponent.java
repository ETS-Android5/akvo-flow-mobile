/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.injector.component;

import android.content.Context;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.util.GsonMapper;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.injector.module.ViewModule;
import org.akvo.flow.presentation.BaseActivity;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.service.UserRequestedApkUpdateService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class, ViewModule.class
})
public interface ApplicationComponent {

    ThreadExecutor getThreadExecutor();

    PostExecutionThread getPostExecutionThread();

    Context context();

    ApkRepository apkRepository();

    GsonMapper gsonMapper();

    void inject(FlowApp app);

    void inject(BaseActivity baseActivity);

    void inject(UserRequestedApkUpdateService userRequestedApkUpdateService);

    void inject(ApkUpdateService apkUpdateService);

}
