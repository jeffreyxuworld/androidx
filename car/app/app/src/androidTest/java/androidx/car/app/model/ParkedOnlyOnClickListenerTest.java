/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;

import androidx.car.app.host.OnDoneCallback;
import androidx.car.app.host.model.OnClickListenerWrapper;
import androidx.car.app.host.model.OnClickListenerWrapperImpl;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link OnClickListenerWrapper}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ParkedOnlyOnClickListenerTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    OnClickListener mMockOnClickListener;

    @Test
    @UiThreadTest
    public void create() throws RemoteException {
        ParkedOnlyOnClickListener parkedOnlyOnClickListener =
                ParkedOnlyOnClickListener.create(mMockOnClickListener);
        OnClickListenerWrapper wrapper =
                OnClickListenerWrapperImpl.create(parkedOnlyOnClickListener);

        assertThat(wrapper.isParkedOnly()).isTrue();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);

        wrapper.onClick(onDoneCallback);
        verify(mMockOnClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }
}
