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

package org.akvo.flow.data.datasource.files;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

@Singleton
public class ImageDataSource {

    private final MediaResolverHelper mediaResolverHelper;
    private final BitmapHelper bitmapHelper;

    @Inject
    public ImageDataSource(MediaResolverHelper mediaResolverHelper, BitmapHelper bitmapHelper) {
        this.mediaResolverHelper = mediaResolverHelper;
        this.bitmapHelper = bitmapHelper;
    }

    public Observable<Boolean> saveImages(Bitmap bitmap, String originalFilePath,
            String resizedFilePath) {
        return Observable.zip(saveBitmap(bitmap, originalFilePath),
                saveResizedBitmap(bitmap, resizedFilePath),
                new BiFunction<Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean savedImage, Boolean savedResizedImage) {
                        return savedImage && savedResizedImage;
                    }
                });
    }

    public Observable<Boolean> copyResizedImage(final Uri uri, final String resizedImagePath,
            final int imageSize, final boolean removeDuplicate) {
        return saveResizedImage(uri, resizedImagePath, imageSize)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean saved) {
                        if (removeDuplicate) {
                            return Observable.just(mediaResolverHelper.removeDuplicateImage(uri));
                        } else {
                            return Observable.just(saved);
                        }
                    }
                });
    }

    private Observable<Boolean> saveResizedImage(final Uri originalImagePath,
            final String resizedImagePath, int imageSize) {
        return resizeImage(originalImagePath, resizedImagePath, imageSize)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean result) {
                        return Observable.just(mediaResolverHelper
                                .updateExifData(originalImagePath, resizedImagePath));
                    }
                });
    }

    private Observable<Boolean> saveResizedBitmap(Bitmap bitmap, String absolutePath) {
        Bitmap resizedBitmap = bitmapHelper.createResizedBitmap(bitmap);
        return saveBitmap(resizedBitmap, absolutePath);
    }

    private Observable<Boolean> resizeImage(Uri uri, String outFilename, int sizePreference) {
        ParcelFileDescriptor parcelFileDescriptor = mediaResolverHelper.openFileDescriptor(uri);
        if (parcelFileDescriptor != null) {
            Bitmap bitmap = bitmapHelper.getBitmap(sizePreference, parcelFileDescriptor);
            return saveBitmap(bitmap, outFilename);
        }
        return Observable.error(new Exception("Error getting bitmap from uri: " + uri));
    }

    private Observable<Boolean> saveBitmap(@Nullable Bitmap bitmap, String filename) {
        if (bitmapHelper.compressBitmap(bitmap, filename)) {
            return Observable.just(true);
        }
        return Observable.error(new Exception("Error saving bitmap"));
    }
}
