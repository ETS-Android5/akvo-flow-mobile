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

package org.akvo.flow.util.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class GlideImageLoader implements ImageLoader {

    private final RequestManager requestManager;

    public GlideImageLoader(Context context) {
        requestManager = Glide.with(context);
    }

    public GlideImageLoader(Activity activity) {
        requestManager = Glide.with(activity);
    }

    public GlideImageLoader(Fragment fragment) {
        requestManager = Glide.with(fragment);
    }

    @Override
    public void loadFromFile(File file, final ImageLoaderListener listener) {
        requestManager.asBitmap().load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model,
                            Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        listener.onImageReady(resource);
                        return false;
                    }
                })
                .submit();
    }

    @Override
    public void loadFromFile(File file, ImageView imageView) {
        requestManager.load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView);
    }

    @Override
    public void loadFromBase64String(String image, ImageView imageView,
            final ImageLoaderListener listener) {
        requestManager
                .load(Base64.decode(image, Base64.DEFAULT))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource,
                            boolean isFirstResource) {
                        listener.onImageReady(null);
                        return false;
                    }
                })
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(imageView);
    }
}
