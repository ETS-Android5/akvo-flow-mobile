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

package org.akvo.flow.presentation.datapoints.list.entity

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class DateMapper @Inject constructor() {

    fun formatDate(timeStamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeStamp
        val calendarNow = Calendar.getInstance()
        return if (calendar.get(Calendar.DATE) == calendarNow.get(Calendar.DATE)) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeFormat.format(Date(timeStamp))
        } else if (calendar.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timeStamp))
        } else {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            dateFormat.format(Date(timeStamp))
        }
    }
}
