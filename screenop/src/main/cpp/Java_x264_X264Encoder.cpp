/*
 * Copyright (c) 2018-present, lmyooyo@gmail.com.
 *
 * This source code is licensed under the GPL license found in the
 * LICENSE file in the root directory of this source tree.
 */
#include "X264Encoder.h"
#include "log.h"

static X264Encoder *encoder;
#ifdef __cplusplus
extern "C" {
#endif

static bool encode(jbyte *src, jbyte *dest, int *size, int *type) {
    bool result = encoder->encode((char *) src, (char *) dest, size, type);
    return result;
}
#ifdef __cplusplus
}
#endif

extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_init(JNIEnv *env, jclass type, jint fmt) {
    encoder = new X264Encoder(fmt);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setVideoSize(JNIEnv *env, jclass type, jint width, jint height) {
    encoder->setVideoSize(width, height);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setBitrate(JNIEnv *env, jclass type, jint bitrate) {
    encoder->setBitrate(bitrate);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setFrameFormat(JNIEnv *env, jclass type, jint format) {
    encoder->setFrameFormat(format);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setFps(JNIEnv *env, jclass type, jint fps) {
    encoder->setFps(fps);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setProfile(JNIEnv *env, jclass type, jstring profile_) {
    const char *profile = env->GetStringUTFChars(profile_, 0);
    encoder->setProfile(const_cast<char *>(profile));
    env->ReleaseStringUTFChars(profile_, profile);
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_setLevel(JNIEnv *env, jclass type, jint level) {
    encoder->setLevel(level);

}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_start(JNIEnv *env, jclass type) {
    if (!encoder->start()) {
        LOGE("X264Encoder start failed!");
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_cry_x264_X264Encoder_stop(JNIEnv *env, jclass type) {
    encoder->stop();
    if (NULL != encoder) {
        delete encoder;
    }
    encoder = NULL;
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_cry_x264_X264Encoder_encode(JNIEnv *env, jclass type_, jbyteArray src_, jbyteArray dest_,
                                     jintArray size_, jintArray type_temp) {
    jbyte *srcBuffer = env->GetByteArrayElements(src_, JNI_FALSE);
    jbyte *destBuffer = env->GetByteArrayElements(dest_, JNI_FALSE);
    jint *pSize = env->GetIntArrayElements(size_, JNI_FALSE);
    jint *pType = env->GetIntArrayElements(type_temp, JNI_FALSE);
    bool result = encode(srcBuffer, destBuffer, pSize, pType);
    env->ReleaseByteArrayElements(src_, srcBuffer, JNI_FALSE);
    env->ReleaseByteArrayElements(dest_, destBuffer, JNI_FALSE);
    env->ReleaseIntArrayElements(size_, pSize, JNI_FALSE);
    env->ReleaseIntArrayElements(type_temp, pType, JNI_FALSE);
    return (jboolean) result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_cry_yuv_YuvUtils_ConvertToI420(JNIEnv *env, jclass type, jbyteArray input_,
                                        jbyteArray output_, jint width, jint height) {
    jbyte *srcBuffer = env->GetByteArrayElements(input_, JNI_FALSE);
    jbyte *destBuffer = env->GetByteArrayElements(output_, JNI_FALSE);
    int y_size = width * height;
    uint8_t *y = reinterpret_cast<uint8_t *>(destBuffer);
    uint8_t *u = y + y_size;
    uint8_t *v = y + y_size * 5 / 4;
    int ret = libyuv::ConvertToI420((const uint8_t *) srcBuffer, width * height,
                                    y, width,
                                    u, width / 2,
                                    v, width / 2,
                                    0, 0,
                                    width, height,
                                    width, height,
                                    libyuv::kRotate0, libyuv::FOURCC_ABGR);
    env->ReleaseByteArrayElements(input_, srcBuffer, JNI_FALSE);
    env->ReleaseByteArrayElements(output_, destBuffer, JNI_FALSE);
    return static_cast<jboolean>(ret >= 0);
}