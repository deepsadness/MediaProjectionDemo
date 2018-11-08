package com.cry.cry.mediaprojectioncode.codec;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.ArrayList;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

/**
 * MediaCodec 工具类
 * Created by a2957 on 4/24/2018.
 */
public class MediaCodecHelper {
    //两种H.264
    public static final String VIDEO_AVC = MIMETYPE_VIDEO_AVC; // H.264 Advanced Video Coding
    public static final String AUDIO_AAC = MIMETYPE_AUDIO_AAC; // H.264 Advanced Audio Coding

    //    //得到制定的MimeType的Codec
//    public static Single<List<MediaCodecInfo>> getAdaptiveEncoderCodec(String mimeType) {
//        return Observable
//                .fromArray(getMediaCodecInfos())
//                .filter(mediaCodecInfo -> {
//                    if (mediaCodecInfo.isEncoder()) {
//                        try {
//                            MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(mimeType);
//                            return capabilitiesForType != null;
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            return false;
//                        }
//                    }
//                    return false;
//                })
//                .toList()
//                ;
//    }
//
    public static MediaCodecInfo[] getMediaCodecInfos() {
        //获取当前可以支持的MediaCodec
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        //得到所有CodecInfos
        return mediaCodecList.getCodecInfos();
    }


    public static ArrayList<MediaCodecInfo> getAdaptiveEncoderCodec(String mimeType) {
        MediaCodecInfo[] mediaCodecInfos = MediaCodecHelper.getMediaCodecInfos();
        ArrayList<MediaCodecInfo> encoderList = new ArrayList<>();
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo.isEncoder()) {
                MediaCodecInfo.CodecCapabilities capabilitiesForType = null;
                try {
                    capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(mimeType);
                } catch (Exception e) {

                }
                if (capabilitiesForType != null) {
                    encoderList.add(mediaCodecInfo);
                }
            }
        }
        return encoderList;
    }
}
