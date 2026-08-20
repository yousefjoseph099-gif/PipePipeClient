package org.schabi.newpipe.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.ThemeHelper;

public class AppearanceSettingsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            removePreference(getString(R.string.player_notification_screen_key));
        }

        setupSettingsPasswordPreference();

        final String themeKey = getString(R.string.theme_key);
        // the key of the active theme when settings were opened (or recreated after theme change)
        final String startThemeKey = defaultPreferences
                .getString(themeKey, getString(R.string.default_theme_value));
        final String autoDeviceThemeKey = getString(R.string.auto_device_theme_key);
        findPreference(themeKey).setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.toString().equals(autoDeviceThemeKey)) {
                Toast.makeText(getContext(), getString(R.string.select_night_theme_toast),
                        Toast.LENGTH_LONG).show();
            }

            applyThemeChange(startThemeKey, themeKey, newValue);
            return false;
        });

        final String nightThemeKey = getString(R.string.night_theme_key);
        if (startThemeKey.equals(autoDeviceThemeKey)) {
            final String startNightThemeKey = defaultPreferences
                    .getString(nightThemeKey, getString(R.string.default_night_theme_value));

            findPreference(nightThemeKey).setOnPreferenceChangeListener((preference, newValue) -> {
                applyThemeChange(startNightThemeKey, nightThemeKey, newValue);
                return false;
            });
        } else {
            removePreference(nightThemeKey);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Settings password
    //////////////////////////////////////////////////////////////////////////*/

    private void setupSettingsPasswordPreference() {
        final Preference passwordPref = findPreference(getString(R.string.settings_password_key));
        if (passwordPref == null) {
            return;
        }
        updateSettingsPasswordSummary(passwordPref);
        passwordPref.setOnPreferenceClickListener(preference -> {
            if (SettingsPasswordUtil.isPasswordSet(requireContext())) {
                promptCurrentPassword(passwordPref);
            } else {
                promptNewPassword(passwordPref);
            }
            return true;
        });
    }

    private void updateSettingsPasswordSummary(final Preference passwordPref) {
        passwordPref.setSummary(SettingsPasswordUtil.isPasswordSet(requireContext())
                ? R.string.settings_password_summary_on
                : R.string.settings_password_summary_off);
    }

    private View inflatePasswordView(final int hintRes) {
        final View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_settings_password, null);
        final EditText input = view.findViewById(R.id.settings_password_input);
        input.setHint(hintRes);
        return view;
    }

    private void promptCurrentPassword(final Preference passwordPref) {
        final View view = inflatePasswordView(R.string.settings_password_enter_current);
        final EditText input = view.findViewById(R.id.settings_password_input);

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_password_title)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.settings_password_remove, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(shown -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (SettingsPasswordUtil.checkPassword(
                        requireContext(), input.getText().toString())) {
                    dialog.dismiss();
                    promptNewPassword(passwordPref);
                } else {
                    input.setError(getString(R.string.settings_password_wrong));
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (SettingsPasswordUtil.checkPassword(
                        requireContext(), input.getText().toString())) {
                    SettingsPasswordUtil.clearPassword(requireContext());
                    updateSettingsPasswordSummary(passwordPref);
                    Toast.makeText(getContext(), R.string.settings_password_removed,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    input.setError(getString(R.string.settings_password_wrong));
                }
            });
        });

        dialog.show();
    }

    private void promptNewPassword(final Preference passwordPref) {
        final View view = inflatePasswordView(R.string.settings_password_enter_new);
        final EditText input = view.findViewById(R.id.settings_password_input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_password_title)
                .setView(view)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    final String newPassword = input.getText().toString();
                    if (newPassword.isEmpty()) {
                        Toast.makeText(getContext(), R.string.settings_password_empty,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    promptConfirmPassword(passwordPref, newPassword);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void promptConfirmPassword(final Preference passwordPref, final String newPassword) {
        final View view = inflatePasswordView(R.string.settings_password_confirm_new);
        final EditText input = view.findViewById(R.id.settings_password_input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_password_title)
                .setView(view)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    if (newPassword.equals(input.getText().toString())) {
                        SettingsPasswordUtil.setPassword(requireContext(), newPassword);
                        updateSettingsPasswordSummary(passwordPref);
                        Toast.makeText(getContext(), R.string.settings_password_saved,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.settings_password_mismatch,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void removePreference(final String preferenceKey) {
        final Preference preference = findPreference(preferenceKey);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    private void applyThemeChange(final String beginningThemeKey,
                                  final String themeKey,
                                  final Object newValue) {
        defaultPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, true).apply();
        defaultPreferences.edit().putString(themeKey, newValue.toString()).apply();

        ThemeHelper.setDayNightMode(getContext(), newValue.toString());

        if (!newValue.equals(beginningThemeKey) && getActivity() != null) {
            // if it's not the current theme
            ActivityCompat.recreate(getActivity());
        }
    }
}
