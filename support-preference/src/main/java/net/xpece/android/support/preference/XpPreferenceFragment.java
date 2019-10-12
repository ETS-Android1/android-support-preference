package net.xpece.android.support.preference;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import net.xpece.android.support.preference.plugins.XpSupportPreferencePlugins;

import java.lang.reflect.Field;

public abstract class XpPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = XpPreferenceFragment.class.getSimpleName();

    public static final String DIALOG_FRAGMENT_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG";

    private static final Field FIELD_PREFERENCE_MANAGER;

    static {
        Field f = null;
        try {
            f = PreferenceFragmentCompat.class.getDeclaredField("mPreferenceManager");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            XpSupportPreferencePlugins.onError(e, "mPreferenceManager not available.");
        }
        FIELD_PREFERENCE_MANAGER = f;
    }

    /**
     * For tracking whether {@link #getContext()} should return the real thing
     * or styled {@link PreferenceManager#getContext()}.
     *
     * AndroidX Preference 1.1.0 injects the preference theme overlay into its activity.
     * We support the case of themed application context with retained fragments.
     * This allows us to achieve that.
     */
    private boolean mCreatingViews = false;

    /**
     * Read and apply the {@link R.attr#preferenceTheme} overlay on top of supplied context.
     */
    @NonNull
    private Context resolveStyledContext(@NonNull final Context context) {
        final int theme = StyledContextProvider.resolveResourceId(context, R.attr.preferenceTheme);
        if (theme == 0) {
            throw new IllegalStateException("Must specify preferenceTheme in theme");
        }
        return new ContextThemeWrapper(context, theme);
    }

    private void setPreferenceManager(@NonNull final PreferenceManager manager) {
        try {
            FIELD_PREFERENCE_MANAGER.set(this, manager);
        } catch (IllegalAccessException e) {
            // This should never happen.
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public String[] getCustomDefaultPackages() {
        return null;
    }

    private void printActivityLeakWarning() {
        Log.w(TAG, "When using setRetainInstance(true) your Activity instance will leak on configuration change.");
        Log.w(TAG, "Override onProvideCustomStyledContext() and provide a custom long-lived context.");
        Log.w(TAG, "You can use methods in " + StyledContextProvider.class + " class.");
    }

    /**
     * If you use retained fragment you won't have to re-inflate the preference hierarchy
     * with each orientation change. On the other hand your original activity context will leak.
     * <p>
     * Use this method to provide your own themed long-lived context.
     *
     * @return Your own base styled context or {@code null} to use standard activity context.
     * @see StyledContextProvider#getThemedApplicationContext(Activity)
     * @see StyledContextProvider#getActivityThemeResource(Activity)
     */
    @Nullable
    protected ContextThemeWrapper onProvideCustomStyledContext() {
        return null;
    }

    @NonNull
    private Context getStyledContext() {
        return getPreferenceManager().getContext();
    }

    @Override
    public final void onCreatePreferences(@Nullable final Bundle bundle, @Nullable final String s) {
        onCreatePreferences1();
        onCreatePreferences2(bundle, s);
    }

    void onCreatePreferences1() {
        // Clear the original Preference Manager
        PreferenceManager manager = getPreferenceManager();
        manager.setOnNavigateToScreenListener(null);

        Context styledContext;
        final ContextThemeWrapper customStyledContext = onProvideCustomStyledContext();
        if (customStyledContext != null) {
            styledContext = resolveStyledContext(customStyledContext);
            styledContext = ActivityAwareContext.wrapIfNecessary(styledContext, this);
        } else {
            if (getRetainInstance()) {
                printActivityLeakWarning();
            }
            styledContext = getStyledContext();
        }

        // Setup custom Preference Manager
        manager = new XpPreferenceManager(styledContext, getCustomDefaultPackages());
        setPreferenceManager(manager);
        manager.setOnNavigateToScreenListener(this);
    }

    public abstract void onCreatePreferences2(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mCreatingViews = true;
        try {
            return super.onCreateView(inflater, container, savedInstanceState);
        } finally {
            mCreatingViews = false;
        }
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        mCreatingViews = true;
        try {
            super.addPreferencesFromResource(preferencesResId);
        } finally {
            mCreatingViews = false;
        }
    }

    @Override
    public void setPreferencesFromResource(int preferencesResId, @Nullable String key) {
        mCreatingViews = true;
        try {
            super.setPreferencesFromResource(preferencesResId, key);
        } finally {
            mCreatingViews = false;
        }
    }

    @Nullable
    @Override
    public Context getContext() {
        if (mCreatingViews) {
            return getStyledContext();
        } else {
            return super.getContext();
        }
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull final Preference preference) {
        boolean handled = false;

        // This has to be done first. Doubled call in super :(
        if (this.getCallbackFragment() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            handled = ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getCallbackFragment()).onPreferenceDisplayDialog(this, preference);
        }
        if (!handled && this.getActivity() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            handled = ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getActivity()).onPreferenceDisplayDialog(this, preference);
        }

        // Handling custom preferences.
        if (!handled) {
            if (this.getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) == null) {
                DialogFragment f;
                if (preference instanceof EditTextPreference) {
                    f = XpEditTextPreferenceDialogFragment.newInstance(preference.getKey());
                } else if (preference instanceof ListPreference) {
                    f = XpListPreferenceDialogFragment.newInstance(preference.getKey());
                } else if (preference instanceof MultiSelectListPreference) {
                    f = XpMultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
                } else if (preference instanceof SeekBarDialogPreference) {
                    f = XpSeekBarPreferenceDialogFragment.newInstance(preference.getKey());
                } else if (preference instanceof RingtonePreference) {
                    final RingtonePreference ringtonePreference = (RingtonePreference) preference;
                    final Context context = ringtonePreference.getContext();
                    final boolean canPlayDefault = ringtonePreference.canPlayDefaultRingtone(context);
                    final boolean canShowSelectedTitle = ringtonePreference.canShowSelectedRingtoneTitle(context);
                    if ((!canPlayDefault || !canShowSelectedTitle) &&
                            ringtonePreference.getOnFailedToReadRingtoneListener() != null) {
                        ringtonePreference.getOnFailedToReadRingtoneListener()
                                .onFailedToReadRingtone(ringtonePreference, canPlayDefault, canShowSelectedTitle);
                        return;
                    } else {
                        f = XpRingtonePreferenceDialogFragment.newInstance(preference.getKey());
                    }
                } else {
                    super.onDisplayPreferenceDialog(preference);
                    return;
                }

                f.setTargetFragment(this, 0);
                f.show(this.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        }
    }

    @NonNull
    @Override
    protected RecyclerView.Adapter onCreateAdapter(@NonNull final PreferenceScreen preferenceScreen) {
        return new XpPreferenceGroupAdapter(preferenceScreen);
    }

    @Nullable
    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setPreferenceScreen(null);
    }

    /**
     * This allows to start activities from application context without the
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} flag.
     */
    private static class ActivityAwareContext extends ContextWrapper {

        /**
         * If supplied context is derived from an activity it returns the supplied context.
         * Otherwise {@code startActivity} calls will be routed to {@code fragment.getActivity()}.
         *
         * @param base     A context.
         * @param fragment A fragment for obtaining host activity.
         * @return A context able to start activities without {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
         */
        static Context wrapIfNecessary(final Context base, final Fragment fragment) {
            for (Context i = base; i instanceof ContextWrapper; i = ((ContextWrapper) i).getBaseContext()) {
                if (i instanceof Activity) return base;
            }
            return new ActivityAwareContext(base, fragment);
        }

        private final Fragment mFragment;

        private ActivityAwareContext(final Context base, final Fragment fragment) {
            super(base);
            mFragment = fragment;
        }

        @NonNull
        private FragmentActivity getActivity() {
            final FragmentActivity activity = mFragment.getActivity();
            if (activity == null) {
                throw new IllegalStateException(mFragment + " is not attached to activity.");
            }
            return activity;
        }

        @Override
        public void startActivities(final Intent[] intents) {
            getActivity().startActivities(intents);
        }

        @Override
        @RequiresApi(16)
        public void startActivities(final Intent[] intents, @Nullable final Bundle options) {
            getActivity().startActivities(intents, options);
        }

        @Override
        public void startActivity(final Intent intent) {
            getActivity().startActivity(intent);
        }

        @Override
        @RequiresApi(16)
        public void startActivity(final Intent intent, @Nullable final Bundle options) {
            getActivity().startActivity(intent, options);
        }
    }
}
