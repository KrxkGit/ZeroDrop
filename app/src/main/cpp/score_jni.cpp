#include <jni.h>
#include <android/log.h>
#include "score_fsm.h"

#define LOG_TAG "ZeroDropFSM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static zerodrop::ScoreFsm* getFsm(JNIEnv* env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(thiz, fid);
    return reinterpret_cast<zerodrop::ScoreFsm*>(ptr);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeInit(JNIEnv* env, jobject thiz) {
    auto* fsm = new zerodrop::ScoreFsm();
    LOGI("ScoreFsm created at %p", fsm);
    return reinterpret_cast<jlong>(fsm);
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeDestroy(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    LOGI("ScoreFsm destroyed at %p", fsm);
    delete fsm;
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID(clazz, "nativePtr", "J");
    env->SetLongField(thiz, fid, 0);
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeSetup(JNIEnv* env, jobject thiz, jint scoreLimit) {
    auto* fsm = getFsm(env, thiz);
    fsm->init(static_cast<int>(scoreLimit));
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeScoreLeft(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    return fsm->scoreLeft() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeScoreRight(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    return fsm->scoreRight() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeUndo(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    return fsm->undo() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeEnterEditMode(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    fsm->enterEditMode();
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeSetEditScores(
    JNIEnv* env, jobject thiz, jint left, jint right, jint serveSide) {
    auto* fsm = getFsm(env, thiz);
    return fsm->setEditScores(left, right, serveSide) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeConfirmEdit(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    fsm->confirmEdit();
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeConfirmSideSwitch(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    fsm->confirmSideSwitch();
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeGetStateSnapshot(JNIEnv* env, jobject thiz, jintArray outArray) {
    auto* fsm = getFsm(env, thiz);
    jint* arr = env->GetIntArrayElements(outArray, nullptr);
    fsm->getStateIntArray(arr);
    env->ReleaseIntArrayElements(outArray, arr, 0);
}

JNIEXPORT void JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeRestoreState(JNIEnv* env, jobject thiz, jintArray inArray) {
    auto* fsm = getFsm(env, thiz);
    jint* arr = env->GetIntArrayElements(inArray, nullptr);
    fsm->restoreFromIntArray(arr);
    env->ReleaseIntArrayElements(inArray, arr, JNI_ABORT);
}

JNIEXPORT jstring JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeSerialize(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    std::string data = fsm->serialize();
    return env->NewStringUTF(data.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeDeserialize(JNIEnv* env, jobject thiz, jstring data) {
    auto* fsm = getFsm(env, thiz);
    const char* utf = env->GetStringUTFChars(data, nullptr);
    bool ok = fsm->deserialize(std::string(utf));
    env->ReleaseStringUTFChars(data, utf);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeIsGamePoint(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    const auto& snap = fsm->currentSnapshot();
    return zerodrop::ScoreFsm::isGamePoint(snap.scoreLimit, snap.leftScore, snap.rightScore)
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_zerodrop_app_ScoreBridge_nativeGetHistorySize(JNIEnv* env, jobject thiz) {
    auto* fsm = getFsm(env, thiz);
    return static_cast<jint>(fsm->historySize());
}

} // extern "C"
