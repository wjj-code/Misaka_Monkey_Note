package com.transsion.ledger.ui.voice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 备研 — App 内 SpeechRecognizer 服务探测与绑定（已停用，语音记账改用语义输入法）。
 * 兼容国产机 / Android 11+ 包可见性限制的语音识别能力探测。
 */
@Deprecated
public final class SpeechRecognitionHelper {

    private static final String[] PREFERRED_PACKAGES = {
            "com.xiaomi.mibrain.speech",
            "com.miui.voiceassist",
            "com.google.android.googlequicksearchbox",
            "com.google.android.tts",
            "com.baidu.input",
            "com.sohu.inputmethod.sogou",
            "com.iflytek.inputmethod",
            "com.huawei.vassistant",
            "com.vivo.agent",
            "com.oppo.speechassist",
            "com.transsion.aivoiceassistant"
    };

    private SpeechRecognitionHelper() {}

    /** 默认使用应用内 SpeechRecognizer；系统 AsrActivity 在 MIUI 上易报「出错了(2)」 */
    public static boolean shouldPreferSystemUi(Context context) {
        return false;
    }

    public static boolean isAvailable(Context context) {
        return !findRecognitionServices(context).isEmpty()
                || hasSystemRecognizerActivity(context)
                || SpeechRecognizer.isRecognitionAvailable(context);
    }

    public static boolean hasSystemRecognizerActivity(Context context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    public static List<ComponentName> findRecognitionServices(Context context) {
        Set<ComponentName> found = new LinkedHashSet<>();
        PackageManager pm = context.getPackageManager();

        Intent serviceIntent = new Intent(RecognitionService.SERVICE_INTERFACE);
        List<ResolveInfo> services = pm.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA);
        if (services != null) {
            for (ResolveInfo info : services) {
                if (info.serviceInfo != null) {
                    found.add(new ComponentName(
                            info.serviceInfo.packageName, info.serviceInfo.name));
                }
            }
        }

        List<ComponentName> ordered = new ArrayList<>();
        for (String pkg : PREFERRED_PACKAGES) {
            for (ComponentName c : found) {
                if (pkg.equals(c.getPackageName()) && !ordered.contains(c)) {
                    ordered.add(c);
                }
            }
        }
        for (ComponentName c : found) {
            if (!ordered.contains(c)) {
                ordered.add(c);
            }
        }
        return ordered;
    }

    public static SpeechRecognizer createRecognizer(Context context) {
        for (ComponentName component : findRecognitionServices(context)) {
            try {
                SpeechRecognizer recognizer =
                        SpeechRecognizer.createSpeechRecognizer(context, component);
                if (recognizer != null) {
                    return recognizer;
                }
            } catch (Exception ignored) {
            }
        }
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(context);
                if (recognizer != null) {
                    return recognizer;
                }
            } catch (Exception ignored) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
