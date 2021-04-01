/*
 * Copyright (C) 2018,2021 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.domain.interactor.apk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.akvo.flow.domain.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

class SaveApkUpdateNotified @Inject constructor(
   private val userRepository: UserRepository
) {

    suspend fun execute() {
        withContext(Dispatchers.IO) {
            try {
                userRepository.saveLastNotificationTime()
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
}