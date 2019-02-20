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

package org.akvo.flow.data.datasource.files;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import org.akvo.flow.data.util.FileHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class MediaResolverHelperTest {

    @Mock
    Context mockContext;

    @Mock
    ContentResolver mockContentResolver;

    @Mock
    ExifHelper mockExifHelper;

    @Mock
    FileHelper mockFileHelper;

    @Mock
    Uri mockUri;

    @Mock
    InputStream mockInputStream;

    @Mock
    Cursor mockCursor;

    private MediaResolverHelper helper;

    @Before
    public void setUp() {
        helper = spy(new MediaResolverHelper(mockContext, mockExifHelper, mockFileHelper));
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);

        PowerMockito.mockStatic(TextUtils.class);
        when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    @Test
    public void getInputStreamFromUriShouldReturnNullWhenUriNotFound()
            throws FileNotFoundException {
        when(mockContentResolver.openInputStream(any(Uri.class)))
                .thenThrow(FileNotFoundException.class);
        when(mockUri.toString()).thenReturn("");

        InputStream inputStream = helper.getInputStreamFromUri(mockUri);
        assertNull(inputStream);
    }

    @Test
    public void openFileDescriptorShouldReturnNullWhenUriNotFound() throws FileNotFoundException {
        when(mockContentResolver.openFileDescriptor(any(Uri.class), anyString()))
                .thenThrow(FileNotFoundException.class);
        when(mockUri.toString()).thenReturn("");

        ParcelFileDescriptor fileDescriptor = helper.openFileDescriptor(mockUri);
        assertNull(fileDescriptor);
    }

    @Test
    public void removeDuplicateImageShouldCallRemoveDuplicatedExtraFileIfNonEmptyPath()
            throws FileNotFoundException {
        when(mockContentResolver.openInputStream(any(Uri.class))).thenReturn(mockInputStream);
        when(mockContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.DATE_TAKEN
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getString(anyInt())).thenReturn("abc");
        when(mockCursor.getColumnIndex(anyString())).thenReturn(0);
        when(mockExifHelper.areDatesEqual(any(InputStream.class), any(InputStream.class)))
                .thenReturn(false);
        doNothing().when(mockFileHelper).close(any(Closeable.class));
        when(mockContentResolver.delete(mockUri, null, null)).thenReturn(1);

        boolean result = helper.removeDuplicateImage(mockUri);

        assertTrue(result);
        verify(helper, times(1)).removeDuplicatedExtraFile(mockUri, "abc");
    }

    @Test
    public void removeDuplicateImageShouldNotCallRemoveDuplicatedExtraFileIfEmptyPath()
            throws FileNotFoundException {
        when(mockContentResolver.openInputStream(any(Uri.class))).thenReturn(mockInputStream);
        when(mockContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.DATE_TAKEN
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")).thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(false);
        when(mockContentResolver.delete(mockUri, null, null)).thenReturn(1);

        boolean result = helper.removeDuplicateImage(mockUri);

        assertTrue(result);
        verify(helper, times(0)).removeDuplicatedExtraFile(mockUri, "abc");
    }

    @Test
    public void deleteMediaShouldReturnFalseIfNothingDeleted() {
        when(mockContentResolver.delete(mockUri, null, null)).thenReturn(0);

        boolean deleted = helper.deleteMedia(mockUri);

        assertFalse(deleted);
    }

    @Test
    public void deleteMediaShouldReturnTrueIfDeleted() {
        when(mockContentResolver.delete(mockUri, null, null)).thenReturn(1);

        boolean deleted = helper.deleteMedia(mockUri);

        assertTrue(deleted);
    }
}
