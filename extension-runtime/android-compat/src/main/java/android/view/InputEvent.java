/*
 * Copyright (C) 2010 The Android Open Source Project
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

// Modified for Katari's desktop extension host from the Android Open Source Project's InputEvent: the SDK's members,
// so the MotionEvent extensions build can be made; key events stay the SDK stub's. Events don't travel in parcels.

package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class InputEvent implements Parcelable {
    public static final Parcelable.Creator<InputEvent> CREATOR = new Parcelable.Creator<InputEvent>() {
        public InputEvent createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        public InputEvent[] newArray(int size) {
            return new InputEvent[size];
        }
    };

    protected boolean mRecycled;

    InputEvent() {
    }

    public abstract int getDeviceId();

    public final InputDevice getDevice() {
        return InputDevice.getDevice(getDeviceId());
    }

    public abstract int getSource();

    public boolean isFromSource(int source) {
        return (getSource() & source) == source;
    }

    public abstract long getEventTime();

    public int describeContents() {
        return 0;
    }

    void recycle() {
        if (mRecycled) {
            throw new RuntimeException(toString() + " recycled twice!");
        }
        mRecycled = true;
    }
}
