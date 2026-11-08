/*
 * Copyright (C) 2007 The Android Open Source Project
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

// Modified for Katari's desktop extension host from the Android Open Source Project's MotionEvent: the SDK's members
// without their documentation, which AOSP's has, kept in Java fields where AOSP keeps them in a native event. An event
// is what an extension builds with obtain and dispatches to its web view (android.webkit.WebView, which passes it on
// to its page as touch input); nothing on desktop reports motion. An event holds no history, so addBatch,
// transform and the parcel throw as the SDK's stub does, and the historical getters with them.

package android.view;

import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;


public final class MotionEvent extends InputEvent implements Parcelable {
    public static final int INVALID_POINTER_ID = -1;

    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;
    public static final int ACTION_HOVER_MOVE = 7;
    public static final int ACTION_SCROLL = 8;
    public static final int ACTION_HOVER_ENTER = 9;
    public static final int ACTION_HOVER_EXIT = 10;
    public static final int ACTION_BUTTON_PRESS = 11;
    public static final int ACTION_BUTTON_RELEASE = 12;
    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
    @Deprecated
    public static final int ACTION_POINTER_1_DOWN = ACTION_POINTER_DOWN | 0x0000;
    @Deprecated
    public static final int ACTION_POINTER_2_DOWN = ACTION_POINTER_DOWN | 0x0100;
    @Deprecated
    public static final int ACTION_POINTER_3_DOWN = ACTION_POINTER_DOWN | 0x0200;
    @Deprecated
    public static final int ACTION_POINTER_1_UP = ACTION_POINTER_UP | 0x0000;
    @Deprecated
    public static final int ACTION_POINTER_2_UP = ACTION_POINTER_UP | 0x0100;
    @Deprecated
    public static final int ACTION_POINTER_3_UP = ACTION_POINTER_UP | 0x0200;
    @Deprecated
    public static final int ACTION_POINTER_ID_MASK = 0xff00;
    @Deprecated
    public static final int ACTION_POINTER_ID_SHIFT = 8;

    public static final int FLAG_WINDOW_IS_OBSCURED = 0x1;
    public static final int FLAG_WINDOW_IS_PARTIALLY_OBSCURED = 0x2;
    public static final int FLAG_CANCELED = 0x20;

    public static final int EDGE_TOP = 0x00000001;
    public static final int EDGE_BOTTOM = 0x00000002;
    public static final int EDGE_LEFT = 0x00000004;
    public static final int EDGE_RIGHT = 0x00000008;

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_PRESSURE = 2;
    public static final int AXIS_SIZE = 3;
    public static final int AXIS_TOUCH_MAJOR = 4;
    public static final int AXIS_TOUCH_MINOR = 5;
    public static final int AXIS_TOOL_MAJOR = 6;
    public static final int AXIS_TOOL_MINOR = 7;
    public static final int AXIS_ORIENTATION = 8;
    public static final int AXIS_VSCROLL = 9;
    public static final int AXIS_HSCROLL = 10;
    public static final int AXIS_Z = 11;
    public static final int AXIS_RX = 12;
    public static final int AXIS_RY = 13;
    public static final int AXIS_RZ = 14;
    public static final int AXIS_HAT_X = 15;
    public static final int AXIS_HAT_Y = 16;
    public static final int AXIS_LTRIGGER = 17;
    public static final int AXIS_RTRIGGER = 18;
    public static final int AXIS_THROTTLE = 19;
    public static final int AXIS_RUDDER = 20;
    public static final int AXIS_WHEEL = 21;
    public static final int AXIS_GAS = 22;
    public static final int AXIS_BRAKE = 23;
    public static final int AXIS_DISTANCE = 24;
    public static final int AXIS_TILT = 25;
    public static final int AXIS_SCROLL = 26;
    public static final int AXIS_RELATIVE_X = 27;
    public static final int AXIS_RELATIVE_Y = 28;
    public static final int AXIS_GENERIC_1 = 32;
    public static final int AXIS_GENERIC_2 = 33;
    public static final int AXIS_GENERIC_3 = 34;
    public static final int AXIS_GENERIC_4 = 35;
    public static final int AXIS_GENERIC_5 = 36;
    public static final int AXIS_GENERIC_6 = 37;
    public static final int AXIS_GENERIC_7 = 38;
    public static final int AXIS_GENERIC_8 = 39;
    public static final int AXIS_GENERIC_9 = 40;
    public static final int AXIS_GENERIC_10 = 41;
    public static final int AXIS_GENERIC_11 = 42;
    public static final int AXIS_GENERIC_12 = 43;
    public static final int AXIS_GENERIC_13 = 44;
    public static final int AXIS_GENERIC_14 = 45;
    public static final int AXIS_GENERIC_15 = 46;
    public static final int AXIS_GENERIC_16 = 47;
    public static final int AXIS_GESTURE_X_OFFSET = 48;
    public static final int AXIS_GESTURE_Y_OFFSET = 49;
    public static final int AXIS_GESTURE_SCROLL_X_DISTANCE = 50;
    public static final int AXIS_GESTURE_SCROLL_Y_DISTANCE = 51;
    public static final int AXIS_GESTURE_PINCH_SCALE_FACTOR = 52;

    public static final int BUTTON_PRIMARY = 1 << 0;
    public static final int BUTTON_SECONDARY = 1 << 1;
    public static final int BUTTON_TERTIARY = 1 << 2;
    public static final int BUTTON_BACK = 1 << 3;
    public static final int BUTTON_FORWARD = 1 << 4;
    public static final int BUTTON_STYLUS_PRIMARY = 1 << 5;
    public static final int BUTTON_STYLUS_SECONDARY = 1 << 6;

    public static final int CLASSIFICATION_NONE = 0;
    public static final int CLASSIFICATION_AMBIGUOUS_GESTURE = 1;
    public static final int CLASSIFICATION_DEEP_PRESS = 2;
    public static final int CLASSIFICATION_TWO_FINGER_SWIPE = 3;
    public static final int CLASSIFICATION_PINCH = 5;

    public static final int TOOL_TYPE_UNKNOWN = 0;
    public static final int TOOL_TYPE_FINGER = 1;
    public static final int TOOL_TYPE_STYLUS = 2;
    public static final int TOOL_TYPE_MOUSE = 3;
    public static final int TOOL_TYPE_ERASER = 4;

    public static final Parcelable.Creator<MotionEvent> CREATOR = new Parcelable.Creator<MotionEvent>() {
        public MotionEvent createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        public MotionEvent[] newArray(int size) {
            return new MotionEvent[size];
        }
    };

    private static final long NS_PER_MS = 1000000;
    private static final String LABEL_PREFIX = "AXIS_";
    private static final int DEFAULT_DISPLAY = 0;

    private static final String[] AXIS_SYMBOLIC_NAMES = new String[AXIS_GESTURE_PINCH_SCALE_FACTOR + 1];

    static {
        String[] names = AXIS_SYMBOLIC_NAMES;
        names[AXIS_X] = "AXIS_X";
        names[AXIS_Y] = "AXIS_Y";
        names[AXIS_PRESSURE] = "AXIS_PRESSURE";
        names[AXIS_SIZE] = "AXIS_SIZE";
        names[AXIS_TOUCH_MAJOR] = "AXIS_TOUCH_MAJOR";
        names[AXIS_TOUCH_MINOR] = "AXIS_TOUCH_MINOR";
        names[AXIS_TOOL_MAJOR] = "AXIS_TOOL_MAJOR";
        names[AXIS_TOOL_MINOR] = "AXIS_TOOL_MINOR";
        names[AXIS_ORIENTATION] = "AXIS_ORIENTATION";
        names[AXIS_VSCROLL] = "AXIS_VSCROLL";
        names[AXIS_HSCROLL] = "AXIS_HSCROLL";
        names[AXIS_Z] = "AXIS_Z";
        names[AXIS_RX] = "AXIS_RX";
        names[AXIS_RY] = "AXIS_RY";
        names[AXIS_RZ] = "AXIS_RZ";
        names[AXIS_HAT_X] = "AXIS_HAT_X";
        names[AXIS_HAT_Y] = "AXIS_HAT_Y";
        names[AXIS_LTRIGGER] = "AXIS_LTRIGGER";
        names[AXIS_RTRIGGER] = "AXIS_RTRIGGER";
        names[AXIS_THROTTLE] = "AXIS_THROTTLE";
        names[AXIS_RUDDER] = "AXIS_RUDDER";
        names[AXIS_WHEEL] = "AXIS_WHEEL";
        names[AXIS_GAS] = "AXIS_GAS";
        names[AXIS_BRAKE] = "AXIS_BRAKE";
        names[AXIS_DISTANCE] = "AXIS_DISTANCE";
        names[AXIS_TILT] = "AXIS_TILT";
        names[AXIS_SCROLL] = "AXIS_SCROLL";
        names[AXIS_RELATIVE_X] = "AXIS_RELATIVE_X";
        names[AXIS_RELATIVE_Y] = "AXIS_RELATIVE_Y";
        names[AXIS_GENERIC_1] = "AXIS_GENERIC_1";
        names[AXIS_GENERIC_2] = "AXIS_GENERIC_2";
        names[AXIS_GENERIC_3] = "AXIS_GENERIC_3";
        names[AXIS_GENERIC_4] = "AXIS_GENERIC_4";
        names[AXIS_GENERIC_5] = "AXIS_GENERIC_5";
        names[AXIS_GENERIC_6] = "AXIS_GENERIC_6";
        names[AXIS_GENERIC_7] = "AXIS_GENERIC_7";
        names[AXIS_GENERIC_8] = "AXIS_GENERIC_8";
        names[AXIS_GENERIC_9] = "AXIS_GENERIC_9";
        names[AXIS_GENERIC_10] = "AXIS_GENERIC_10";
        names[AXIS_GENERIC_11] = "AXIS_GENERIC_11";
        names[AXIS_GENERIC_12] = "AXIS_GENERIC_12";
        names[AXIS_GENERIC_13] = "AXIS_GENERIC_13";
        names[AXIS_GENERIC_14] = "AXIS_GENERIC_14";
        names[AXIS_GENERIC_15] = "AXIS_GENERIC_15";
        names[AXIS_GENERIC_16] = "AXIS_GENERIC_16";
        names[AXIS_GESTURE_X_OFFSET] = "AXIS_GESTURE_X_OFFSET";
        names[AXIS_GESTURE_Y_OFFSET] = "AXIS_GESTURE_Y_OFFSET";
        names[AXIS_GESTURE_SCROLL_X_DISTANCE] = "AXIS_GESTURE_SCROLL_X_DISTANCE";
        names[AXIS_GESTURE_SCROLL_Y_DISTANCE] = "AXIS_GESTURE_SCROLL_Y_DISTANCE";
        names[AXIS_GESTURE_PINCH_SCALE_FACTOR] = "AXIS_GESTURE_PINCH_SCALE_FACTOR";
    }

    private int mDeviceId;
    private int mSource;
    private int mDisplayId;
    private int mAction;
    private int mFlags;
    private int mEdgeFlags;
    private int mMetaState;
    private int mButtonState;
    private int mClassification;
    private float mXOffset;
    private float mYOffset;
    private float mXPrecision;
    private float mYPrecision;
    private long mDownTimeNanos;
    private long mEventTimeNanos;
    private PointerProperties[] mPointerProperties;
    private PointerCoords[] mPointerCoords;

    private MotionEvent() {
    }

    public static MotionEvent obtain(long downTime, long eventTime,
            int action, int pointerCount, PointerProperties[] pointerProperties,
            PointerCoords[] pointerCoords, int metaState, int buttonState,
            float xPrecision, float yPrecision, int deviceId,
            int edgeFlags, int source, int displayId, int flags, int classification) {
        if (pointerCount < 1) {
            throw new IllegalArgumentException("pointerCount must be at least 1");
        }
        if (pointerProperties == null || pointerProperties.length < pointerCount) {
            throw new IllegalArgumentException("pointerProperties array must be large enough to hold all pointers");
        }
        if (pointerCoords == null || pointerCoords.length < pointerCount) {
            throw new IllegalArgumentException("pointerCoords array must be large enough to hold all pointers");
        }
        MotionEvent ev = new MotionEvent();
        ev.mDeviceId = deviceId;
        ev.mSource = source;
        ev.mDisplayId = displayId;
        ev.mAction = action;
        ev.mFlags = flags;
        ev.mEdgeFlags = edgeFlags;
        ev.mMetaState = metaState;
        ev.mButtonState = buttonState;
        ev.mClassification = classification;
        ev.mXPrecision = xPrecision;
        ev.mYPrecision = yPrecision;
        ev.mDownTimeNanos = downTime * NS_PER_MS;
        ev.mEventTimeNanos = eventTime * NS_PER_MS;
        ev.mPointerProperties = new PointerProperties[pointerCount];
        ev.mPointerCoords = new PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            ev.mPointerProperties[i] = new PointerProperties(pointerProperties[i]);
            ev.mPointerCoords[i] = new PointerCoords(pointerCoords[i]);
        }
        return ev;
    }

    public static MotionEvent obtain(long downTime, long eventTime,
            int action, int pointerCount, PointerProperties[] pointerProperties,
            PointerCoords[] pointerCoords, int metaState, int buttonState,
            float xPrecision, float yPrecision, int deviceId,
            int edgeFlags, int source, int displayId, int flags) {
        return obtain(downTime, eventTime, action, pointerCount, pointerProperties, pointerCoords,
                metaState, buttonState, xPrecision, yPrecision, deviceId, edgeFlags, source,
                displayId, flags, CLASSIFICATION_NONE);
    }

    public static MotionEvent obtain(long downTime, long eventTime,
            int action, int pointerCount, PointerProperties[] pointerProperties,
            PointerCoords[] pointerCoords, int metaState, int buttonState,
            float xPrecision, float yPrecision, int deviceId,
            int edgeFlags, int source, int flags) {
        return obtain(downTime, eventTime, action, pointerCount, pointerProperties, pointerCoords,
                metaState, buttonState, xPrecision, yPrecision, deviceId, edgeFlags, source,
                DEFAULT_DISPLAY, flags);
    }

    @Deprecated
    public static MotionEvent obtain(long downTime, long eventTime,
            int action, int pointerCount, int[] pointerIds, PointerCoords[] pointerCoords,
            int metaState, float xPrecision, float yPrecision, int deviceId,
            int edgeFlags, int source, int flags) {
        PointerProperties[] pp = PointerProperties.createArray(pointerCount);
        for (int i = 0; i < pointerCount; i++) {
            pp[i].id = pointerIds[i];
        }
        return obtain(downTime, eventTime, action, pointerCount, pp,
                pointerCoords, metaState, 0, xPrecision, yPrecision, deviceId,
                edgeFlags, source, flags);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action,
            float x, float y, float pressure, float size, int metaState,
            float xPrecision, float yPrecision, int deviceId, int edgeFlags) {
        return obtain(downTime, eventTime, action, x, y, pressure, size, metaState,
                xPrecision, yPrecision, deviceId, edgeFlags, InputDevice.SOURCE_CLASS_POINTER,
                DEFAULT_DISPLAY);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action,
            float x, float y, float pressure, float size, int metaState,
            float xPrecision, float yPrecision, int deviceId, int edgeFlags, int source,
            int displayId) {
        PointerProperties[] pp = PointerProperties.createArray(1);
        pp[0].id = 0;
        PointerCoords[] pc = PointerCoords.createArray(1);
        pc[0].x = x;
        pc[0].y = y;
        pc[0].pressure = pressure;
        pc[0].size = size;
        return obtain(downTime, eventTime, action, 1, pp, pc, metaState, 0, xPrecision, yPrecision,
                deviceId, edgeFlags, source, displayId, 0, CLASSIFICATION_NONE);
    }

    @Deprecated
    public static MotionEvent obtain(long downTime, long eventTime, int action,
            int pointerCount, float x, float y, float pressure, float size, int metaState,
            float xPrecision, float yPrecision, int deviceId, int edgeFlags) {
        return obtain(downTime, eventTime, action, x, y, pressure, size,
                metaState, xPrecision, yPrecision, deviceId, edgeFlags);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action,
            float x, float y, int metaState) {
        return obtain(downTime, eventTime, action, x, y, 1.0f, 1.0f,
                metaState, 1.0f, 1.0f, 0, 0);
    }

    public static MotionEvent obtain(MotionEvent other) {
        return obtainNoHistory(other);
    }

    public static MotionEvent obtainNoHistory(MotionEvent other) {
        if (other == null) {
            throw new IllegalArgumentException("other motion event must not be null");
        }
        MotionEvent ev = new MotionEvent();
        ev.mDeviceId = other.mDeviceId;
        ev.mSource = other.mSource;
        ev.mDisplayId = other.mDisplayId;
        ev.mAction = other.mAction;
        ev.mFlags = other.mFlags;
        ev.mEdgeFlags = other.mEdgeFlags;
        ev.mMetaState = other.mMetaState;
        ev.mButtonState = other.mButtonState;
        ev.mClassification = other.mClassification;
        ev.mXOffset = other.mXOffset;
        ev.mYOffset = other.mYOffset;
        ev.mXPrecision = other.mXPrecision;
        ev.mYPrecision = other.mYPrecision;
        ev.mDownTimeNanos = other.mDownTimeNanos;
        ev.mEventTimeNanos = other.mEventTimeNanos;
        int count = other.getPointerCount();
        ev.mPointerProperties = new PointerProperties[count];
        ev.mPointerCoords = new PointerCoords[count];
        for (int i = 0; i < count; i++) {
            ev.mPointerProperties[i] = new PointerProperties(other.mPointerProperties[i]);
            ev.mPointerCoords[i] = new PointerCoords(other.mPointerCoords[i]);
        }
        return ev;
    }

    @Override
    public void recycle() {
        super.recycle();
    }

    @Override
    public int getDeviceId() {
        return mDeviceId;
    }

    @Override
    public int getSource() {
        return mSource;
    }

    public void setSource(int source) {
        mSource = source;
    }

    public int getAction() {
        return mAction;
    }

    public int getActionMasked() {
        return mAction & ACTION_MASK;
    }

    public int getActionIndex() {
        return (mAction & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public void setAction(int action) {
        mAction = action;
    }

    public int getFlags() {
        return mFlags;
    }

    public long getDownTime() {
        return mDownTimeNanos / NS_PER_MS;
    }

    @Override
    public long getEventTime() {
        return mEventTimeNanos / NS_PER_MS;
    }

    public long getEventTimeNanos() {
        return mEventTimeNanos;
    }

    public float getX() {
        return getX(0);
    }

    public float getY() {
        return getY(0);
    }

    public float getPressure() {
        return getPressure(0);
    }

    public float getSize() {
        return getSize(0);
    }

    public float getTouchMajor() {
        return getTouchMajor(0);
    }

    public float getTouchMinor() {
        return getTouchMinor(0);
    }

    public float getToolMajor() {
        return getToolMajor(0);
    }

    public float getToolMinor() {
        return getToolMinor(0);
    }

    public float getOrientation() {
        return getOrientation(0);
    }

    public float getAxisValue(int axis) {
        return getAxisValue(axis, 0);
    }

    public int getPointerCount() {
        return mPointerCoords.length;
    }

    public int getPointerId(int pointerIndex) {
        return pointer(pointerIndex).id;
    }

    public int getToolType(int pointerIndex) {
        return pointer(pointerIndex).toolType;
    }

    public int findPointerIndex(int pointerId) {
        for (int i = 0; i < mPointerProperties.length; i++) {
            if (mPointerProperties[i].id == pointerId) {
                return i;
            }
        }
        return -1;
    }

    public float getX(int pointerIndex) {
        return coords(pointerIndex).x + mXOffset;
    }

    public float getY(int pointerIndex) {
        return coords(pointerIndex).y + mYOffset;
    }

    public float getPressure(int pointerIndex) {
        return coords(pointerIndex).pressure;
    }

    public float getSize(int pointerIndex) {
        return coords(pointerIndex).size;
    }

    public float getTouchMajor(int pointerIndex) {
        return coords(pointerIndex).touchMajor;
    }

    public float getTouchMinor(int pointerIndex) {
        return coords(pointerIndex).touchMinor;
    }

    public float getToolMajor(int pointerIndex) {
        return coords(pointerIndex).toolMajor;
    }

    public float getToolMinor(int pointerIndex) {
        return coords(pointerIndex).toolMinor;
    }

    public float getOrientation(int pointerIndex) {
        return coords(pointerIndex).orientation;
    }

    public float getAxisValue(int axis, int pointerIndex) {
        switch (axis) {
            case AXIS_X:
                return getX(pointerIndex);
            case AXIS_Y:
                return getY(pointerIndex);
            default:
                return coords(pointerIndex).getAxisValue(axis);
        }
    }

    public void getPointerProperties(int pointerIndex, PointerProperties outPointerProperties) {
        outPointerProperties.copyFrom(pointer(pointerIndex));
    }

    public void getPointerCoords(int pointerIndex, PointerCoords outPointerCoords) {
        outPointerCoords.copyFrom(coords(pointerIndex));
        outPointerCoords.x += mXOffset;
        outPointerCoords.y += mYOffset;
    }

    public int getMetaState() {
        return mMetaState;
    }

    public int getButtonState() {
        return mButtonState;
    }

    public int getClassification() {
        return mClassification;
    }

    public int getActionButton() {
        return 0;
    }

    public boolean isButtonPressed(int button) {
        if (button == 0) {
            return false;
        }
        return (getButtonState() & button) == button;
    }

    public float getRawX() {
        return getRawX(0);
    }

    public float getRawY() {
        return getRawY(0);
    }

    public float getRawX(int pointerIndex) {
        return coords(pointerIndex).x;
    }

    public float getRawY(int pointerIndex) {
        return coords(pointerIndex).y;
    }

    public float getXPrecision() {
        return mXPrecision;
    }

    public float getYPrecision() {
        return mYPrecision;
    }

    public int getEdgeFlags() {
        return mEdgeFlags;
    }

    public void setEdgeFlags(int flags) {
        mEdgeFlags = flags;
    }

    public void offsetLocation(float deltaX, float deltaY) {
        mXOffset += deltaX;
        mYOffset += deltaY;
    }

    public void setLocation(float x, float y) {
        offsetLocation(x - getX(), y - getY());
    }

    public int getHistorySize() {
        return 0;
    }

    public long getHistoricalEventTime(int pos) {
        throw new RuntimeException("Stub!");
    }

    public long getHistoricalEventTimeNanos(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalX(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalY(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalPressure(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalSize(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalTouchMajor(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalTouchMinor(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalToolMajor(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalToolMinor(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalOrientation(int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalAxisValue(int axis, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalX(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalY(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalPressure(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalSize(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalTouchMajor(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalTouchMinor(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalToolMajor(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalToolMinor(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalOrientation(int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public float getHistoricalAxisValue(int axis, int pointerIndex, int pos) {
        throw new RuntimeException("Stub!");
    }

    public void getHistoricalPointerCoords(int pointerIndex, int historicalIndex, PointerCoords outPointerCoords) {
        throw new RuntimeException("Stub!");
    }

    public void addBatch(long eventTime, float x, float y, float pressure, float size, int metaState) {
        throw new RuntimeException("Stub!");
    }

    public void addBatch(long eventTime, PointerCoords[] pointerCoords, int metaState) {
        throw new RuntimeException("Stub!");
    }

    public void transform(Matrix matrix) {
        throw new RuntimeException("Stub!");
    }

    public void writeToParcel(Parcel out, int flags) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder();
        msg.append("MotionEvent { action=").append(actionToString(getAction()));
        final int pointerCount = getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            msg.append(", id[").append(i).append("]=").append(getPointerId(i));
            msg.append(", x[").append(i).append("]=").append(getX(i));
            msg.append(", y[").append(i).append("]=").append(getY(i));
            msg.append(", toolType[").append(i).append("]=").append(getToolType(i));
        }
        msg.append(", buttonState=").append(getButtonState());
        msg.append(", metaState=").append(getMetaState());
        msg.append(", flags=0x").append(Integer.toHexString(getFlags()));
        msg.append(", edgeFlags=0x").append(Integer.toHexString(getEdgeFlags()));
        msg.append(", pointerCount=").append(pointerCount);
        msg.append(", historySize=").append(getHistorySize());
        msg.append(", eventTime=").append(getEventTime());
        msg.append(", downTime=").append(getDownTime());
        msg.append(", deviceId=").append(getDeviceId());
        msg.append(", source=0x").append(Integer.toHexString(getSource()));
        msg.append(", displayId=").append(mDisplayId);
        msg.append(" }");
        return msg.toString();
    }

    public static String actionToString(int action) {
        switch (action) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
            case ACTION_BUTTON_PRESS:
                return "ACTION_BUTTON_PRESS";
            case ACTION_BUTTON_RELEASE:
                return "ACTION_BUTTON_RELEASE";
        }
        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        switch (action & ACTION_MASK) {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }

    public static String axisToString(int axis) {
        String symbolicName = axis >= 0 && axis < AXIS_SYMBOLIC_NAMES.length ? AXIS_SYMBOLIC_NAMES[axis] : null;
        return symbolicName != null ? symbolicName : Integer.toString(axis);
    }

    public static int axisFromString(String symbolicName) {
        if (symbolicName.startsWith(LABEL_PREFIX)) {
            for (int axis = 0; axis < AXIS_SYMBOLIC_NAMES.length; axis++) {
                if (symbolicName.equals(AXIS_SYMBOLIC_NAMES[axis])) {
                    return axis;
                }
            }
            symbolicName = symbolicName.substring(LABEL_PREFIX.length());
        }
        try {
            return Integer.parseInt(symbolicName, 10);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private PointerProperties pointer(int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= mPointerProperties.length) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        return mPointerProperties[pointerIndex];
    }

    private PointerCoords coords(int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= mPointerCoords.length) {
            throw new IllegalArgumentException("pointerIndex out of range");
        }
        return mPointerCoords[pointerIndex];
    }

    public static final class PointerCoords {
        private static final int INITIAL_PACKED_AXIS_VALUES = 8;
        private long mPackedAxisBits;
        private float[] mPackedAxisValues;
        private float mRelativeX;
        private float mRelativeY;

        public float x;
        public float y;
        public float pressure;
        public float size;
        public float touchMajor;
        public float touchMinor;
        public float toolMajor;
        public float toolMinor;
        public float orientation;

        public PointerCoords() {
        }

        public PointerCoords(PointerCoords other) {
            copyFrom(other);
        }

        static PointerCoords[] createArray(int size) {
            PointerCoords[] array = new PointerCoords[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerCoords();
            }
            return array;
        }

        // Nothing on desktop resamples motion
        public boolean isResampled() {
            return false;
        }

        public void clear() {
            mPackedAxisBits = 0;
            x = 0;
            y = 0;
            pressure = 0;
            size = 0;
            touchMajor = 0;
            touchMinor = 0;
            toolMajor = 0;
            toolMinor = 0;
            orientation = 0;
            mRelativeX = 0;
            mRelativeY = 0;
        }

        public void copyFrom(PointerCoords other) {
            final long bits = other.mPackedAxisBits;
            mPackedAxisBits = bits;
            if (bits != 0) {
                final float[] otherValues = other.mPackedAxisValues;
                final int count = Long.bitCount(bits);
                float[] values = mPackedAxisValues;
                if (values == null || count > values.length) {
                    values = new float[otherValues.length];
                    mPackedAxisValues = values;
                }
                System.arraycopy(otherValues, 0, values, 0, count);
            }
            x = other.x;
            y = other.y;
            pressure = other.pressure;
            size = other.size;
            touchMajor = other.touchMajor;
            touchMinor = other.touchMinor;
            toolMajor = other.toolMajor;
            toolMinor = other.toolMinor;
            orientation = other.orientation;
            mRelativeX = other.mRelativeX;
            mRelativeY = other.mRelativeY;
        }

        public float getAxisValue(int axis) {
            switch (axis) {
                case AXIS_X:
                    return x;
                case AXIS_Y:
                    return y;
                case AXIS_PRESSURE:
                    return pressure;
                case AXIS_SIZE:
                    return size;
                case AXIS_TOUCH_MAJOR:
                    return touchMajor;
                case AXIS_TOUCH_MINOR:
                    return touchMinor;
                case AXIS_TOOL_MAJOR:
                    return toolMajor;
                case AXIS_TOOL_MINOR:
                    return toolMinor;
                case AXIS_ORIENTATION:
                    return orientation;
                case AXIS_RELATIVE_X:
                    return mRelativeX;
                case AXIS_RELATIVE_Y:
                    return mRelativeY;
                default: {
                    if (axis < 0 || axis > 63) {
                        throw new IllegalArgumentException("Axis out of range.");
                    }
                    final long bits = mPackedAxisBits;
                    final long axisBit = 0x8000000000000000L >>> axis;
                    if ((bits & axisBit) == 0) {
                        return 0;
                    }
                    final int index = Long.bitCount(bits & ~(0xFFFFFFFFFFFFFFFFL >>> axis));
                    return mPackedAxisValues[index];
                }
            }
        }

        public void setAxisValue(int axis, float value) {
            switch (axis) {
                case AXIS_X:
                    x = value;
                    break;
                case AXIS_Y:
                    y = value;
                    break;
                case AXIS_PRESSURE:
                    pressure = value;
                    break;
                case AXIS_SIZE:
                    size = value;
                    break;
                case AXIS_TOUCH_MAJOR:
                    touchMajor = value;
                    break;
                case AXIS_TOUCH_MINOR:
                    touchMinor = value;
                    break;
                case AXIS_TOOL_MAJOR:
                    toolMajor = value;
                    break;
                case AXIS_TOOL_MINOR:
                    toolMinor = value;
                    break;
                case AXIS_ORIENTATION:
                    orientation = value;
                    break;
                case AXIS_RELATIVE_X:
                    mRelativeX = value;
                    break;
                case AXIS_RELATIVE_Y:
                    mRelativeY = value;
                    break;
                default: {
                    if (axis < 0 || axis > 63) {
                        throw new IllegalArgumentException("Axis out of range.");
                    }
                    final long bits = mPackedAxisBits;
                    final long axisBit = 0x8000000000000000L >>> axis;
                    final int index = Long.bitCount(bits & ~(0xFFFFFFFFFFFFFFFFL >>> axis));
                    float[] values = mPackedAxisValues;
                    if ((bits & axisBit) == 0) {
                        if (values == null) {
                            values = new float[INITIAL_PACKED_AXIS_VALUES];
                            mPackedAxisValues = values;
                        } else {
                            final int count = Long.bitCount(bits);
                            if (count < values.length) {
                                if (index != count) {
                                    System.arraycopy(values, index, values, index + 1, count - index);
                                }
                            } else {
                                float[] newValues = new float[count * 2];
                                System.arraycopy(values, 0, newValues, 0, index);
                                System.arraycopy(values, index, newValues, index + 1, count - index);
                                values = newValues;
                                mPackedAxisValues = values;
                            }
                        }
                        mPackedAxisBits = bits | axisBit;
                    }
                    values[index] = value;
                }
            }
        }
    }

    public static final class PointerProperties {
        public int id;
        public int toolType;

        public PointerProperties() {
            clear();
        }

        public PointerProperties(PointerProperties other) {
            copyFrom(other);
        }

        static PointerProperties[] createArray(int size) {
            PointerProperties[] array = new PointerProperties[size];
            for (int i = 0; i < size; i++) {
                array[i] = new PointerProperties();
            }
            return array;
        }

        public void clear() {
            id = INVALID_POINTER_ID;
            toolType = TOOL_TYPE_UNKNOWN;
        }

        public void copyFrom(PointerProperties other) {
            id = other.id;
            toolType = other.toolType;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof PointerProperties) {
                PointerProperties properties = (PointerProperties) other;
                return id == properties.id && toolType == properties.toolType;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id | (toolType << 8);
        }
    }
}
