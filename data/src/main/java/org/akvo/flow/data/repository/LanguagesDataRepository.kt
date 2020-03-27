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

package org.akvo.flow.data.repository

import io.reactivex.Completable
import io.reactivex.Single
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.domain.repository.LanguagesRepository
import javax.inject.Inject

class LanguagesDataRepository @Inject constructor(private val dataSourceFactory: DataSourceFactory) :
    LanguagesRepository {

    override fun getSavedLanguages(surveyId: Long): Single<Set<String>> {
        return dataSourceFactory.dataBaseDataSource.getSavedLanguages(surveyId)
    }

    override fun saveLanguages(surveyId: Long, languages: Set<String>): Completable {
        return dataSourceFactory.dataBaseDataSource.saveLanguages(surveyId, languages)
    }
}