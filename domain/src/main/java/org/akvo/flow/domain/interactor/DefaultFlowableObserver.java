/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Default subscriber base class to be used whenever you want to avoid having to implement all
 * 3 methods.
 */
public class DefaultFlowableObserver<T> implements Consumer<T>, Action {

    public void onComplete() {
        // no-op by default.
    }

    public void onNext(T t) {
        // no-op by default.
    }

    @Override
    public void run() throws Exception {
        onComplete();
    }

    @Override
    public void accept(@NonNull T t) throws Exception {
        onNext(t);
    }
}
