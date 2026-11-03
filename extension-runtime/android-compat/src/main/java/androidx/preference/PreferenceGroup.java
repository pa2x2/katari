/*
 * Copyright 2018 The Android Open Source Project
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

// Modified for Katari's desktop extension host from AndroidX Preference 1.2.1: the preference model only, without
// views, XML inflation, styled attributes or saved instance state.

package androidx.preference;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PreferenceGroup extends Preference {
    private static final String TAG = "PreferenceGroup";

    private final List<Preference> mPreferences;
    private boolean mOrderingAsAdded = true;
    private int mCurrentPreferenceOrder = 0;
    private boolean mAttachedToHierarchy = false;
    private int mInitialExpandedChildrenCount = Integer.MAX_VALUE;
    private OnExpandButtonClickListener mOnExpandButtonClickListener = null;

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPreferences = new ArrayList<>();
    }

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreferenceGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void setOrderingAsAdded(boolean orderingAsAdded) {
        mOrderingAsAdded = orderingAsAdded;
    }

    public boolean isOrderingAsAdded() {
        return mOrderingAsAdded;
    }

    public void setInitialExpandedChildrenCount(int expandedCount) {
        if (expandedCount != Integer.MAX_VALUE && !hasKey()) {
            Log.e(TAG, getClass().getSimpleName()
                    + " should have a key defined if it contains an expandable preference");
        }
        mInitialExpandedChildrenCount = expandedCount;
    }

    public int getInitialExpandedChildrenCount() {
        return mInitialExpandedChildrenCount;
    }

    public void addItemFromInflater(Preference preference) {
        addPreference(preference);
    }

    public int getPreferenceCount() {
        return mPreferences.size();
    }

    public Preference getPreference(int index) {
        return mPreferences.get(index);
    }

    // AndroidX recycles a removed key's id for a moment so its list keeps a stable row; there is no list here, so each
    // added preference takes the next id.
    public boolean addPreference(Preference preference) {
        if (mPreferences.contains(preference)) {
            return true;
        }
        if (preference.getKey() != null) {
            PreferenceGroup root = this;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            final String key = preference.getKey();
            if (root.findPreference(key) != null) {
                Log.e(TAG, "Found duplicated key: \"" + key
                        + "\". This can cause unintended behaviour,"
                        + " please use unique keys for every preference.");
            }
        }
        if (preference.getOrder() == DEFAULT_ORDER) {
            if (mOrderingAsAdded) {
                preference.setOrder(mCurrentPreferenceOrder++);
            }
            if (preference instanceof PreferenceGroup) {
                ((PreferenceGroup) preference).setOrderingAsAdded(mOrderingAsAdded);
            }
        }
        int insertionIndex = Collections.binarySearch(mPreferences, preference);
        if (insertionIndex < 0) {
            insertionIndex = insertionIndex * -1 - 1;
        }
        if (!onPrepareAddPreference(preference)) {
            return false;
        }
        synchronized (this) {
            mPreferences.add(insertionIndex, preference);
        }
        final PreferenceManager preferenceManager = getPreferenceManager();
        preference.onAttachedToHierarchy(preferenceManager, preferenceManager.getNextId());
        preference.assignParent(this);
        if (mAttachedToHierarchy) {
            preference.onAttached();
        }
        notifyHierarchyChanged();
        return true;
    }

    public boolean removePreference(Preference preference) {
        final boolean returnValue = removePreferenceInt(preference);
        notifyHierarchyChanged();
        return returnValue;
    }

    public boolean removePreferenceRecursively(CharSequence key) {
        final Preference preference = findPreference(key);
        if (preference == null) {
            return false;
        }
        return preference.getParent().removePreference(preference);
    }

    private boolean removePreferenceInt(Preference preference) {
        synchronized (this) {
            preference.onPrepareForRemoval();
            if (preference.getParent() == this) {
                preference.assignParent(null);
            }
            boolean success = mPreferences.remove(preference);
            if (success && mAttachedToHierarchy) {
                preference.onDetached();
            }
            return success;
        }
    }

    public void removeAll() {
        synchronized (this) {
            List<Preference> preferences = mPreferences;
            for (int i = preferences.size() - 1; i >= 0; i--) {
                removePreferenceInt(preferences.get(0));
            }
        }
        notifyHierarchyChanged();
    }

    protected boolean onPrepareAddPreference(Preference preference) {
        preference.onParentChanged(this, shouldDisableDependents());
        return true;
    }

    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    public <T extends Preference> T findPreference(CharSequence key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (TextUtils.equals(getKey(), key)) {
            return (T) this;
        }
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = getPreference(i);
            final String curKey = preference.getKey();
            if (TextUtils.equals(curKey, key)) {
                return (T) preference;
            }
            if (preference instanceof PreferenceGroup) {
                final T returnedPreference = ((PreferenceGroup) preference).findPreference(key);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            }
        }
        return null;
    }

    protected boolean isOnSameScreenAsChildren() {
        return true;
    }

    public boolean isAttached() {
        return mAttachedToHierarchy;
    }

    public void setOnExpandButtonClickListener(OnExpandButtonClickListener onExpandButtonClickListener) {
        mOnExpandButtonClickListener = onExpandButtonClickListener;
    }

    public OnExpandButtonClickListener getOnExpandButtonClickListener() {
        return mOnExpandButtonClickListener;
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mAttachedToHierarchy = true;
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onAttached();
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mAttachedToHierarchy = false;
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onDetached();
        }
    }

    @Override
    public void notifyDependencyChange(boolean disableDependents) {
        super.notifyDependencyChange(disableDependents);
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onParentChanged(this, disableDependents);
        }
    }

    void sortPreferences() {
        synchronized (this) {
            Collections.sort(mPreferences);
        }
    }

    public interface PreferencePositionCallback {
        int getPreferenceAdapterPosition(String key);

        int getPreferenceAdapterPosition(Preference preference);
    }

    public interface OnExpandButtonClickListener {
        void onExpandButtonClick();
    }
}
