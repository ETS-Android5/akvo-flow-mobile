/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
import io.reactivex.observers.TestObserver
import org.akvo.flow.data.datasource.DataSourceFactory
import org.akvo.flow.data.datasource.files.FileDataSource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.runners.MockitoJUnitRunner
import org.powermock.api.mockito.PowerMockito.`when`
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class FileDataRepositoryTest {

    @Mock
    internal var mockDataSourceFactory: DataSourceFactory? = null

    @Mock
    internal var mockFileDataSource: FileDataSource? = null

    @Mock
    internal var mockFile: File? = null

    @Before
    fun setUp() {
        `when`(mockDataSourceFactory!!.fileDataSource).thenReturn(mockFileDataSource)
    }

    @Test
    fun moveFilesShouldCompleteSuccessfully() {
        `when`(mockFileDataSource!!.moveZipFiles()).thenReturn(Completable.complete())
        `when`(mockFileDataSource!!.moveMediaFiles()).thenReturn(Completable.complete())

        val observer = TestObserver<Void>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.moveFiles().subscribe(observer)

        verify(mockFileDataSource, times(1))!!.moveZipFiles()
        verify(mockFileDataSource, times(1))!!.moveMediaFiles()
        observer.assertNoErrors()
    }

    @Test
    fun moveFilesShouldFailIfMoveZipFilesFails() {
        val exception = Exception("test")
        `when`(mockFileDataSource!!.moveZipFiles()).thenReturn(Completable.error(exception))
        `when`(mockFileDataSource!!.moveMediaFiles()).thenReturn(Completable.complete())

        val observer = TestObserver<Void>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.moveFiles().subscribe(observer)

        verify(mockFileDataSource, times(1))!!.moveZipFiles()
        verify(mockFileDataSource, times(1))!!.moveMediaFiles()
        observer.assertError(exception)
    }

    @Test
    fun moveFilesShouldFailIfMoveMediaFilesFails() {
        val exception = Exception("test")
        `when`(mockFileDataSource!!.moveZipFiles()).thenReturn(Completable.complete())
        `when`(mockFileDataSource!!.moveMediaFiles()).thenReturn(Completable.error(exception))

        val observer = TestObserver<Void>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.moveFiles().subscribe(observer)

        verify(mockFileDataSource, times(1))!!.moveZipFiles()
        verify(mockFileDataSource, times(1))!!.moveMediaFiles()
        observer.assertError(exception)
    }

    @Test(expected = NullPointerException::class)
    fun getZipFileShouldThrowNPEIfNullUuid() {
        val observer = TestObserver<File>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.getZipFile(null).subscribe(observer)
    }

    @Test
    fun getZipFileShouldFail() {
        val exception = Exception("test")
        `when`(mockFileDataSource!!.getZipFile(Matchers.anyString())).thenReturn(
            Single.error(
                exception
            )
        )

        val observer = TestObserver<File>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.getZipFile("123").subscribe(observer)

        verify(mockFileDataSource, times(1))!!.getZipFile("123")
        observer.assertError(exception)
    }

    @Test
    fun getZipFileShouldReturnCorrectFile() {
        `when`(mockFileDataSource!!.getZipFile(Matchers.anyString())).thenReturn(
            Single.just(
                mockFile
            )
        )

        val observer = TestObserver<File>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.getZipFile("123").subscribe(observer)

        verify(mockFileDataSource, times(1))!!.getZipFile("123")
        observer.assertNoErrors()
        observer.assertResult(mockFile)
    }

    @Test
    fun createDataZipShouldFail() {
        val exception = Exception("test")
        `when`(
            mockFileDataSource!!.writeDataToZipFile(
                Matchers.anyString(),
                Matchers.anyString()
            )
        ).thenReturn(Completable.error(exception))

        val observer = TestObserver<Void>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.createDataZip("123", "12345").subscribe(observer)

        verify(mockFileDataSource, times(1))!!.writeDataToZipFile("123", "12345")
        observer.assertError(exception)
    }

    @Test
    fun createDataZipShouldCompleteSuccessfully() {
        `when`(
            mockFileDataSource!!.writeDataToZipFile(
                Matchers.anyString(),
                Matchers.anyString()
            )
        ).thenReturn(Completable.complete())

        val observer = TestObserver<Void>()
        val fileDataRep = FileDataRepository(mockDataSourceFactory)

        fileDataRep.createDataZip("123", "12345").subscribe(observer)

        verify(mockFileDataSource, times(1))!!.writeDataToZipFile("123", "12345")
        observer.assertNoErrors()
    }
}