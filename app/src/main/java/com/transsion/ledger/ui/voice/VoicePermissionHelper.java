package com.transsion.ledger.ui.voice;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * 备研 — App 内麦克风 RECORD_AUDIO 权限校验（已停用，语音记账改用语义输入法）。
 * 以 {@link ContextCompat#checkSelfPermission} 为准。
 */
@Deprecated
public final class VoicePermissionHelper {

    private VoicePermissionHelper() {}

    public static boolean hasRecordAudio(Context context) {
        if (context == null) return false;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldOpenPermissionSettings(Context context) {
        return !hasRecordAudio(context);
    }

    public static void openAppSettings(Fragment fragment) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", fragment.requireContext().getPackageName(), null));
        fragment.startActivity(intent);
    }
}
