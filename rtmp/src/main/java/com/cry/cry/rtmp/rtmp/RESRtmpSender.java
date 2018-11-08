package com.cry.cry.rtmp.rtmp;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.cry.cry.rtmp.RtmpClient;
import com.cry.cry.rtmp.sender.Packager;
import com.cry.cry.rtmp.sender.model.RESCoreParameters;
import com.cry.cry.rtmp.sender.tools.LogTools;

import java.nio.ByteBuffer;

public class RESRtmpSender {

    private long jniRtmpPointer;
    private String serverIpAddr;
    private FLvMetaData fLvMetaData;
    private long startTime;

    public long rtmpOpen(String url, int mWidth, int mHeight) {
        LogTools.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
        jniRtmpPointer = RtmpClient.open(url, true);
        final int openR = jniRtmpPointer == 0 ? 1 : 0;
//        if (openR == 0) {
//        }
        serverIpAddr = RtmpClient.getIpAddr(jniRtmpPointer);
        Log.d("zzx", "serverIpAddr=" + serverIpAddr);
        if (jniRtmpPointer != 0) {
            RESCoreParameters coreParameters = new RESCoreParameters();
            coreParameters.mediacodecAVCFrameRate = 15;
            coreParameters.videoWidth = mWidth;
            coreParameters.videoHeight = mHeight;
            fLvMetaData = new FLvMetaData(coreParameters);
            byte[] MetaData = fLvMetaData.getMetaData();
            RtmpClient.write(jniRtmpPointer,
                    MetaData,
                    MetaData.length,
                    RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
//            state = STATE.RUNNING;
        }
        return jniRtmpPointer;
    }

    public void rtmpSendFormat(MediaFormat newFormat) {
        RESFlvData resFlvData = sendAVCDecoderConfigurationRecord(0, newFormat);
        rtmpPublish(resFlvData);
    }

    public void rtmpSend(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        if (info.size == 0) {
            return;
        }
        if (startTime == 0) {
            startTime = info.presentationTimeUs / 1000;
        }
        //rtmp
        outputBuffer.position(info.offset + 4);
//        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + info.size);
        RESFlvData resFlvData = sendRealData((info.presentationTimeUs / 1000) - startTime, outputBuffer);
        rtmpPublish(resFlvData);
    }

    public RESFlvData getRealData(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        if (info.size == 0) {
            return null;
        }
        if (startTime == 0) {
            startTime = info.presentationTimeUs / 1000;
        }
        //rtmp
        outputBuffer.position(info.offset + 4);
//        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + info.size);
        RESFlvData resFlvData = sendRealData((info.presentationTimeUs / 1000) - startTime, outputBuffer);
        return resFlvData;
    }

    public void rtmpPublish(RESFlvData flvData) {
        if (jniRtmpPointer == 0) {
            return;
        }
        final int res = RtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
        LogTools.d("RtmpClient.write res=" + res + ", length =" + flvData.byteBuffer.length);
    }

    public void rtmpClose() {
        if (jniRtmpPointer == 0) {
            return;
        }
        int errorTime = 0;
        final int closeR = RtmpClient.close(jniRtmpPointer);
        serverIpAddr = null;
    }

    private static RESFlvData sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
        return resFlvData;
//        dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }

    private static RESFlvData sendRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH +
                realDataLength;
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                        Packager.FLVPackager.NALU_HEADER_LENGTH,
                realDataLength);
        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                false,
                frameType == 5,
                realDataLength);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = frameType;
        return resFlvData;
//        dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }
}

