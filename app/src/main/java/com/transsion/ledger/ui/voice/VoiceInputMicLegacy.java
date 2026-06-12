package com.transsion.ledger.ui.voice;

/**
 * 备研档案 — App 内麦克风 + {@link android.speech.SpeechRecognizer} 方案（已停用）。
 *
 * <p>产品已改：用户通过系统输入法自带的「语音转文字」录入，见 {@link VoiceInputSheet}。</p>
 *
 * <p>原实现要点（供后续研究）：</p>
 * <ul>
 *   <li>{@link MicrophonePermissionHost} + {@code ActivityCompat.requestPermissions}</li>
 *   <li>{@link SpeechRecognitionHelper} 绑定小米/谷歌 RecognitionService</li>
 *   <li>按住 🎤 → {@code startListening} / 松手 {@code stopListening}</li>
 *   <li>{@code launchSystemRecognizer} 调起 {@code com.xiaomi.mibrain.speech}</li>
 *   <li>{@link VoicePermissionHelper} 运行时录音权限校验</li>
 * </ul>
 *
 * <p>完整 Java 源码曾为本类同名文件，Git 历史可恢复；同包辅助类 {@link SpeechRecognitionHelper}
 * {@link VoicePermissionHelper} {@link MicrophonePermissionHost} 仍保留未删。</p>
 */
@Deprecated
final class VoiceInputMicLegacy {

    private VoiceInputMicLegacy() {}
}
