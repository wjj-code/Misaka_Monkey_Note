package com.transsion.ledger.ui.voice;

/**
 * 备研 — App 内麦克风动态申请接口（已停用，MainActivity 内实现已注释）。
 * 当前语音记账改用语义输入法语音转文字，不再调用本接口。
 */
public interface MicrophonePermissionHost {

    /**
     * 确保 RECORD_AUDIO 已授予后再执行。
     * 即使系统设置里已开，仍会走一次 Activity 级 requestPermissions 以同步 AppOps。
     */
    void ensureRecordAudio(Runnable onGranted);
}
