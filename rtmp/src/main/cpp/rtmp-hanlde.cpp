#include <rtmp.h>
#include <cstdlib>
#include <cstring>
#include "jni.h"
#include "android/log.h"

#define LOG_TAG "RESRTMP"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)


extern "C"
JNIEXPORT jstring JNICALL
Java_com_cry_cry_rtmp_RtmpClient_getIpAddr(JNIEnv *env, jclass type, jlong rtmpPointer) {
    if (rtmpPointer != 0) {
        RTMP *r = (RTMP *) rtmpPointer;
        return env->NewStringUTF(r->ipaddr);
    } else {
        return env->NewStringUTF("");
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_cry_cry_rtmp_RtmpClient_close(JNIEnv *env, jclass type, jlong rtmpPointer) {
    LOGD("start close");

    RTMP_Close((RTMP *) rtmpPointer);
    RTMP_Free((RTMP *) rtmpPointer);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_cry_cry_rtmp_RtmpClient_write(JNIEnv *env, jclass type_, jlong rtmpPointer,
                                       jbyteArray data_, jint size, jint type, jint ts) {
    jbyte *buffer = env->GetByteArrayElements(data_, NULL);
    LOGD("start write");
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, size);
    RTMPPacket_Reset(packet);
    if (type == RTMP_PACKET_TYPE_INFO) { // metadata
        packet->m_nChannel = 0x03;
    } else if (type == RTMP_PACKET_TYPE_VIDEO) { // video
        packet->m_nChannel = 0x04;
    } else if (type == RTMP_PACKET_TYPE_AUDIO) { //audio
        packet->m_nChannel = 0x05;
    } else {
        packet->m_nChannel = -1;
    }
    RTMP *r = (RTMP *) rtmpPointer;
    packet->m_nInfoField2 = r->m_stream_id;

    LOGD("write data type: %d, ts %d", type, ts);

    memcpy(packet->m_body, buffer, size);
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_hasAbsTimestamp = FALSE;
    packet->m_nTimeStamp = ts;
    packet->m_packetType = type;
    packet->m_nBodySize = size;
    int ret = RTMP_SendPacket((RTMP *) rtmpPointer, packet, 0);
    RTMPPacket_Free(packet);
    free(packet);
    env->ReleaseByteArrayElements(data_, buffer, 0);
    if (!ret) {
        LOGD("end write error %d", ret);
        return ret;
    } else {
        LOGD("end write success");
        return 0;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_cry_cry_rtmp_RtmpClient_open(JNIEnv *env, jclass type, jstring url_,
                                      jboolean isPublishMode) {
    const char *url = env->GetStringUTFChars(url_, 0);
    LOGD("RTMP_OPENING:%s", url);
    RTMP *rtmp = RTMP_Alloc();
    if (rtmp == NULL) {
        LOGD("RTMP_Alloc=NULL");
        return NULL;
    }

    RTMP_Init(rtmp);
    int ret = RTMP_SetupURL(rtmp, const_cast<char *>(url));

    if (!ret) {
        RTMP_Free(rtmp);
        rtmp = NULL;
        LOGD("RTMP_SetupURL=ret");
        return NULL;
    }
    if (isPublishMode) {
        RTMP_EnableWrite(rtmp);
    }

    ret = RTMP_Connect(rtmp, NULL);
    if (!ret) {
        RTMP_Free(rtmp);
        rtmp = NULL;
        LOGD("RTMP_Connect=ret");
        return NULL;
    }
    ret = RTMP_ConnectStream(rtmp, 0);

    if (!ret) {
        ret = RTMP_ConnectStream(rtmp, 0);
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = NULL;
        LOGD("RTMP_ConnectStream=ret");
        return NULL;
    }
    env->ReleaseStringUTFChars(url_, url);
    LOGD("RTMP_OPENED");
    return reinterpret_cast<jlong>(rtmp);

}