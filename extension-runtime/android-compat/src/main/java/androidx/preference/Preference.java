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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Preference implements Comparable<Preference> {
    public static final int DEFAULT_ORDER = Integer.MAX_VALUE;

    private final Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceDataStore mPreferenceDataStore;
    private long mId;
    private boolean mHasId;
    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;
    private int mOrder = DEFAULT_ORDER;
    private int mViewId = 0;
    private CharSequence mTitle;
    private CharSequence mSummary;
    private int mIconResId;
    private Drawable mIcon;
    private String mKey;
    private Intent mIntent;
    private String mFragment;
    private Bundle mExtras;
    private boolean mEnabled = true;
    private boolean mSelectable = true;
    private boolean mRequiresKey;
    private boolean mPersistent = true;
    private String mDependencyKey;
    private Object mDefaultValue;
    private boolean mDependencyMet = true;
    private boolean mParentDependencyMet = true;
    private boolean mVisible = true;
    private boolean mSingleLineTitle = true;
    private boolean mIconSpaceReserved;
    private boolean mCopyingEnabled;
    private boolean mShouldDisableView = true;
    private int mLayoutResId;
    private int mWidgetLayoutResId;
    private OnPreferenceChangeInternalListener mListener;
    private List<Preference> mDependents;
    private PreferenceGroup mParentGroup;
    private boolean mWasDetached;
    @SuppressWarnings("rawtypes")
    private SummaryProvider mSummaryProvider;

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Preference(Context context) {
        this(context, null);
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return null;
    }

    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public void setFragment(String fragment) {
        mFragment = fragment;
    }

    public String getFragment() {
        return mFragment;
    }

    public void setPreferenceDataStore(PreferenceDataStore dataStore) {
        mPreferenceDataStore = dataStore;
    }

    public PreferenceDataStore getPreferenceDataStore() {
        if (mPreferenceDataStore != null) {
            return mPreferenceDataStore;
        } else if (mPreferenceManager != null) {
            return mPreferenceManager.getPreferenceDataStore();
        }
        return null;
    }

    public Bundle getExtras() {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        return mExtras;
    }

    public Bundle peekExtras() {
        return mExtras;
    }

    public void setLayoutResource(int layoutResId) {
        mLayoutResId = layoutResId;
    }

    public final int getLayoutResource() {
        return mLayoutResId;
    }

    public void setWidgetLayoutResource(int widgetLayoutResId) {
        mWidgetLayoutResId = widgetLayoutResId;
    }

    public final int getWidgetLayoutResource() {
        return mWidgetLayoutResId;
    }

    public void setOrder(int order) {
        if (order != mOrder) {
            mOrder = order;
            notifyHierarchyChanged();
        }
    }

    public int getOrder() {
        return mOrder;
    }

    public void setViewId(int viewId) {
        mViewId = viewId;
    }

    public void setTitle(CharSequence title) {
        if (!TextUtils.equals(title, mTitle)) {
            mTitle = title;
            notifyChanged();
        }
    }

    public void setTitle(int titleResId) {
        setTitle(mContext.getString(titleResId));
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public void setIcon(Drawable icon) {
        if (mIcon != icon) {
            mIcon = icon;
            mIconResId = 0;
            notifyChanged();
        }
    }

    // There are no drawable resources on desktop, so the id is kept and no icon loads from it.
    public void setIcon(int iconResId) {
        setIcon((Drawable) null);
        mIconResId = iconResId;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    @SuppressWarnings("unchecked")
    public CharSequence getSummary() {
        if (getSummaryProvider() != null) {
            return getSummaryProvider().provideSummary(this);
        }
        return mSummary;
    }

    public void setSummary(CharSequence summary) {
        if (getSummaryProvider() != null) {
            throw new IllegalStateException("Preference already has a SummaryProvider set.");
        }
        if (!TextUtils.equals(mSummary, summary)) {
            mSummary = summary;
            notifyChanged();
        }
    }

    public void setSummary(int summaryResId) {
        setSummary(mContext.getString(summaryResId));
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public boolean isEnabled() {
        return mEnabled && mDependencyMet && mParentDependencyMet;
    }

    public void setSelectable(boolean selectable) {
        if (mSelectable != selectable) {
            mSelectable = selectable;
            notifyChanged();
        }
    }

    public boolean isSelectable() {
        return mSelectable;
    }

    public void setShouldDisableView(boolean shouldDisableView) {
        if (mShouldDisableView != shouldDisableView) {
            mShouldDisableView = shouldDisableView;
            notifyChanged();
        }
    }

    public boolean getShouldDisableView() {
        return mShouldDisableView;
    }

    public final void setVisible(boolean visible) {
        if (mVisible != visible) {
            mVisible = visible;
            if (mListener != null) {
                mListener.onPreferenceVisibilityChange(this);
            }
        }
    }

    public final boolean isVisible() {
        return mVisible;
    }

    public final boolean isShown() {
        if (!isVisible()) {
            return false;
        }
        if (getPreferenceManager() == null) {
            return false;
        }
        if (this == getPreferenceManager().getPreferenceScreen()) {
            return true;
        }
        PreferenceGroup parent = getParent();
        if (parent == null) {
            return false;
        }
        return parent.isShown();
    }

    long getId() {
        return mId;
    }

    protected void onClick() {}

    public void setKey(String key) {
        mKey = key;
        if (mRequiresKey && !hasKey()) {
            requireKey();
        }
    }

    public String getKey() {
        return mKey;
    }

    void requireKey() {
        if (TextUtils.isEmpty(mKey)) {
            throw new IllegalStateException("Preference does not have a key assigned.");
        }
        mRequiresKey = true;
    }

    public boolean hasKey() {
        return !TextUtils.isEmpty(mKey);
    }

    public boolean isPersistent() {
        return mPersistent;
    }

    protected boolean shouldPersist() {
        return mPreferenceManager != null && isPersistent() && hasKey();
    }

    public void setPersistent(boolean persistent) {
        mPersistent = persistent;
    }

    public void setSingleLineTitle(boolean singleLineTitle) {
        mSingleLineTitle = singleLineTitle;
    }

    public boolean isSingleLineTitle() {
        return mSingleLineTitle;
    }

    public void setIconSpaceReserved(boolean iconSpaceReserved) {
        if (mIconSpaceReserved != iconSpaceReserved) {
            mIconSpaceReserved = iconSpaceReserved;
            notifyChanged();
        }
    }

    public boolean isIconSpaceReserved() {
        return mIconSpaceReserved;
    }

    public void setCopyingEnabled(boolean enabled) {
        if (mCopyingEnabled != enabled) {
            mCopyingEnabled = enabled;
            notifyChanged();
        }
    }

    public boolean isCopyingEnabled() {
        return mCopyingEnabled;
    }

    @SuppressWarnings("rawtypes")
    public final void setSummaryProvider(SummaryProvider summaryProvider) {
        mSummaryProvider = summaryProvider;
        notifyChanged();
    }

    @SuppressWarnings("rawtypes")
    public final SummaryProvider getSummaryProvider() {
        return mSummaryProvider;
    }

    public boolean callChangeListener(Object newValue) {
        return mOnChangeListener == null || mOnChangeListener.onPreferenceChange(this, newValue);
    }

    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        mOnChangeListener = onPreferenceChangeListener;
    }

    public OnPreferenceChangeListener getOnPreferenceChangeListener() {
        return mOnChangeListener;
    }

    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        mOnClickListener = onPreferenceClickListener;
    }

    public OnPreferenceClickListener getOnPreferenceClickListener() {
        return mOnClickListener;
    }

    public void performClick() {
        if (!isEnabled() || !isSelectable()) {
            return;
        }
        onClick();
        if (mOnClickListener != null && mOnClickListener.onPreferenceClick(this)) {
            return;
        }
        PreferenceManager preferenceManager = getPreferenceManager();
        if (preferenceManager != null) {
            PreferenceManager.OnPreferenceTreeClickListener listener =
                    preferenceManager.getOnPreferenceTreeClickListener();
            if (listener != null && listener.onPreferenceTreeClick(this)) {
                return;
            }
        }
        if (mIntent != null) {
            Context context = getContext();
            context.startActivity(mIntent);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public SharedPreferences getSharedPreferences() {
        if (mPreferenceManager == null || getPreferenceDataStore() != null) {
            return null;
        }
        return mPreferenceManager.getSharedPreferences();
    }

    @Override
    public int compareTo(Preference another) {
        if (mOrder != another.mOrder) {
            return mOrder - another.mOrder;
        } else if (mTitle == another.mTitle) {
            return 0;
        } else if (mTitle == null) {
            return 1;
        } else if (another.mTitle == null) {
            return -1;
        } else {
            return mTitle.toString().compareToIgnoreCase(another.mTitle.toString());
        }
    }

    final void setOnPreferenceChangeInternalListener(OnPreferenceChangeInternalListener listener) {
        mListener = listener;
    }

    protected void notifyChanged() {
        if (mListener != null) {
            mListener.onPreferenceChange(this);
        }
    }

    protected void notifyHierarchyChanged() {
        if (mListener != null) {
            mListener.onPreferenceHierarchyChange(this);
        }
    }

    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        mPreferenceManager = preferenceManager;
        if (!mHasId) {
            mId = preferenceManager.getNextId();
        }
        dispatchSetInitialValue();
    }

    protected void onAttachedToHierarchy(PreferenceManager preferenceManager, long id) {
        mId = id;
        mHasId = true;
        try {
            onAttachedToHierarchy(preferenceManager);
        } finally {
            mHasId = false;
        }
    }

    void assignParent(PreferenceGroup parentGroup) {
        if (parentGroup != null && mParentGroup != null) {
            throw new IllegalStateException(
                    "This preference already has a parent. You must remove the existing parent "
                            + "before assigning a new one.");
        }
        mParentGroup = parentGroup;
    }

    public void onAttached() {
        registerDependency();
    }

    public void onDetached() {
        unregisterDependency();
        mWasDetached = true;
    }

    final boolean wasDetached() {
        return mWasDetached;
    }

    final void clearWasDetached() {
        mWasDetached = false;
    }

    private void registerDependency() {
        if (TextUtils.isEmpty(mDependencyKey)) return;
        Preference preference = findPreferenceInHierarchy(mDependencyKey);
        if (preference != null) {
            preference.registerDependent(this);
        } else {
            throw new IllegalStateException("Dependency \"" + mDependencyKey
                    + "\" not found for preference \"" + mKey + "\" (title: \"" + mTitle + "\"");
        }
    }

    private void unregisterDependency() {
        if (mDependencyKey != null) {
            final Preference oldDependency = findPreferenceInHierarchy(mDependencyKey);
            if (oldDependency != null) {
                oldDependency.unregisterDependent(this);
            }
        }
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    protected <T extends Preference> T findPreferenceInHierarchy(String key) {
        if (mPreferenceManager == null) {
            return null;
        }
        return mPreferenceManager.findPreference(key);
    }

    private void registerDependent(Preference dependent) {
        if (mDependents == null) {
            mDependents = new ArrayList<>();
        }
        mDependents.add(dependent);
        dependent.onDependencyChanged(this, shouldDisableDependents());
    }

    private void unregisterDependent(Preference dependent) {
        if (mDependents != null) {
            mDependents.remove(dependent);
        }
    }

    public void notifyDependencyChange(boolean disableDependents) {
        final List<Preference> dependents = mDependents;
        if (dependents == null) {
            return;
        }
        final int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        if (mDependencyMet == disableDependent) {
            mDependencyMet = !disableDependent;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public void onParentChanged(Preference parent, boolean disableChild) {
        if (mParentDependencyMet == disableChild) {
            mParentDependencyMet = !disableChild;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    public void setDependency(String dependencyKey) {
        unregisterDependency();
        mDependencyKey = dependencyKey;
        registerDependency();
    }

    public String getDependency() {
        return mDependencyKey;
    }

    public PreferenceGroup getParent() {
        return mParentGroup;
    }

    protected void onPrepareForRemoval() {
        unregisterDependency();
    }

    public void setDefaultValue(Object defaultValue) {
        mDefaultValue = defaultValue;
    }

    private void dispatchSetInitialValue() {
        if (getPreferenceDataStore() != null) {
            onSetInitialValue(true, mDefaultValue);
            return;
        }
        final boolean shouldPersist = shouldPersist();
        if (!shouldPersist || !getSharedPreferences().contains(mKey)) {
            if (mDefaultValue != null) {
                onSetInitialValue(false, mDefaultValue);
            }
        } else {
            onSetInitialValue(true, null);
        }
    }

    @Deprecated
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        onSetInitialValue(defaultValue);
    }

    protected void onSetInitialValue(Object defaultValue) {}

    private void tryCommit(SharedPreferences.Editor editor) {
        if (mPreferenceManager.shouldCommit()) {
            editor.apply();
        }
    }

    protected boolean persistString(String value) {
        if (!shouldPersist()) {
            return false;
        }
        if (TextUtils.equals(value, getPersistedString(null))) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putString(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putString(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getString(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getString(mKey, defaultReturnValue);
    }

    public boolean persistStringSet(Set<String> values) {
        if (!shouldPersist()) {
            return false;
        }
        if (values.equals(getPersistedStringSet(null))) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putStringSet(mKey, values);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putStringSet(mKey, values);
            tryCommit(editor);
        }
        return true;
    }

    public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getStringSet(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getStringSet(mKey, defaultReturnValue);
    }

    protected boolean persistInt(int value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedInt(~value)) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putInt(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putInt(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getInt(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getInt(mKey, defaultReturnValue);
    }

    protected boolean persistFloat(float value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedFloat(Float.NaN)) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putFloat(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putFloat(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    protected float getPersistedFloat(float defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getFloat(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getFloat(mKey, defaultReturnValue);
    }

    protected boolean persistLong(long value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedLong(~value)) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putLong(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putLong(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    protected long getPersistedLong(long defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getLong(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getLong(mKey, defaultReturnValue);
    }

    protected boolean persistBoolean(boolean value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedBoolean(!value)) {
            return true;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            dataStore.putBoolean(mKey, value);
        } else {
            SharedPreferences.Editor editor = mPreferenceManager.getEditor();
            editor.putBoolean(mKey, value);
            tryCommit(editor);
        }
        return true;
    }

    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        PreferenceDataStore dataStore = getPreferenceDataStore();
        if (dataStore != null) {
            return dataStore.getBoolean(mKey, defaultReturnValue);
        }
        return mPreferenceManager.getSharedPreferences().getBoolean(mKey, defaultReturnValue);
    }

    @Override
    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title).append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary).append(' ');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(Preference preference, Object newValue);
    }

    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(Preference preference);
    }

    interface OnPreferenceChangeInternalListener {
        void onPreferenceChange(Preference preference);

        void onPreferenceHierarchyChange(Preference preference);

        void onPreferenceVisibilityChange(Preference preference);
    }

    public interface SummaryProvider<T extends Preference> {
        CharSequence provideSummary(T preference);
    }
}
