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

package org.akvo.flow.domain.interactor;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;

public class ClearResponses extends UseCase {

    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;

    @Inject
    protected ClearResponses(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, SurveyRepository surveyRepository,
            FileRepository fileRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return Observable.zip(surveyRepository.clearResponses(),
                fileRepository.clearResponseFiles(), new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean clearedResponses, Boolean clearedResponseFiles) {
                        return clearedResponses && clearedResponseFiles;
                    }
                });
    }
}
