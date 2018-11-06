#include <jni.h>
#include <libyuv.h>


#include <jni.h>
#include <string>
#include "rtmp.h"
#include <android/log.h>
#include <malloc.h>
#include <string.h>

#define LOG_TAG "RESRTMP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_cry_rtmp_RtmpClient_open(JNIEnv *env, jclass type, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    LOGD("RTMP_OPENING:%s", url);
    //创建rtmp
    RTMP *rtmp = RTMP_Alloc();

    if (rtmp == NULL) {
        LOGD("RTMP_Alloc=NULL");
        return NULL;
    }

    //初始化
    RTMP_Init(rtmp);
    int ret = RTMP_SetupURL(rtmp, const_cast<char *>(url));

    if (!ret) {
        RTMP_Free(rtmp);
        rtmp = NULL;
        LOGD("RTMP_SetupURL=ret");
        return NULL;
    }

    //开启写入
    RTMP_EnableWrite(rtmp);

    //开始连接
    ret = RTMP_Connect(rtmp, NULL);
    if (!ret) {
        RTMP_Free(rtmp);
        rtmp = NULL;
        LOGD("RTMP_Connect=ret");
        return NULL;
    }

    //开始连接stream
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


extern "C"
JNIEXPORT jint JNICALL
Java_com_cry_rtmp_RtmpClient_close(JNIEnv *env, jclass type, jlong rtmp) {
    RTMP_Close((RTMP *) rtmp);
    RTMP_Free((RTMP *) rtmp);
    return 0;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_cry_rtmp_RtmpClient_write(JNIEnv *env, jclass type_, jlong rtmpPointer,
                                                jbyteArray data_, jint size, jint type, jint ts) {
    jbyte *buffer = env->GetByteArrayElements(data_, NULL);
    //创建RTMPPacket
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, size);
    RTMPPacket_Reset(packet);

    if (type == RTMP_PACKET_TYPE_INFO) {    //metadata
        packet->m_nChannel = 0x03;
    } else if (type == RTMP_PACKET_TYPE_VIDEO) {
        packet->m_nChannel = 0x04;
    } else if (type == RTMP_PACKET_TYPE_AUDIO) {
        packet->m_nChannel = 0x05;
    } else {
        packet->m_nChannel = -1;
    }
    packet->m_nInfoField2 = ((RTMP *) rtmpPointer)->m_stream_id;

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
        LOGD("end write error %d", sockerr);
        return sockerr;
    } else {
        LOGD("end write success");
        return 0;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_cry_rtmp_RtmpClient_getIpAddr(JNIEnv *env, jclass type, jlong rtmp) {

    if (rtmp != 0) {
        RTMP *r = (RTMP *) rtmp;
        return env->NewStringUTF(r->ipaddr);
    } else {
        return env->NewStringUTF("");
    }
}