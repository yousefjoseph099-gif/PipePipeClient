package org.schabi.newpipe.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Stores a salted SHA-256 hash of an optional password used to lock the Settings screen.
 * The plain-text password itself is never written to disk.
 */
public final class SettingsPasswordUtil {

    private SettingsPasswordUtil() {
    }

    private static SharedPreferences prefs(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isPasswordSet(final Context context) {
        final String hash = prefs(context)
                .getString(context.getString(R.string.settings_password_hash_key), null);
        return hash != null && !hash.isEmpty();
    }

    public static void setPassword(final Context context, final String plainPassword) {
        final byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        final String saltString = Base64.encodeToString(salt, Base64.NO_WRAP);
        final String hash = hash(plainPassword, saltString);

        prefs(context).edit()
                .putString(context.getString(R.string.settings_password_salt_key), saltString)
                .putString(context.getString(R.string.settings_password_hash_key), hash)
                .apply();
    }

    public static void clearPassword(final Context context) {
        prefs(context).edit()
                .remove(context.getString(R.string.settings_password_salt_key))
                .remove(context.getString(R.string.settings_password_hash_key))
                .apply();
    }

    public static boolean checkPassword(final Context context, final String plainPassword) {
        final SharedPreferences p = prefs(context);
        final String salt = p
                .getString(context.getString(R.string.settings_password_salt_key), null);
        final String storedHash = p
                .getString(context.getString(R.string.settings_password_hash_key), null);
        if (salt == null || storedHash == null) {
            // no password configured, nothing to check against
            return true;
        }
        return storedHash.equals(hash(plainPassword == null ? "" : plainPassword, salt));
    }

    private static String hash(final String plainPassword, final String saltString) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.decode(saltString, Base64.NO_WRAP));
            final byte[] hashed = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hashed, Base64.NO_WRAP);
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on Android, this cannot happen
            throw new RuntimeException(e);
        }
    }
}
