package com.cry.cry.mediaprojectioncode.codec;

import android.media.MediaCodecInfo;
import android.util.SparseArray;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Created by a2957 on 4/24/2018.
 */

public class MediaCodecLogHelper {

    /*
    将colorformat的名字通过反射得到,存放的集合
     */
    private static SparseArray<String> sColorFormats = new SparseArray<>();
    /*
    对应的profileLevels的映射的名字
     */
    private static SparseArray<String> sAACProfiles = new SparseArray<>();
    private static SparseArray<String> sAVCProfiles = new SparseArray<>();
    private static SparseArray<String> sAVCLevels = new SparseArray<>();


    //得到所有ColorFomat的名字
    private static void initColorFormatFields() {
        // COLOR_
        Field[] fields = MediaCodecInfo.CodecCapabilities.class.getFields();
        for (Field f : fields) {
            if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == 0) {
                continue;
            }
            String name = f.getName();
            if (name.startsWith("COLOR_")) {
                try {
                    int value = f.getInt(null);
                    sColorFormats.put(value, name);
                } catch (IllegalAccessException e) {
                    // ignored
                }
            }
        }
    }

    private static void initProfileLevels() {
        Field[] fields = MediaCodecInfo.CodecProfileLevel.class.getFields();
        for (Field f : fields) {
            if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == 0) {
                continue;
            }
            String name = f.getName();
            SparseArray<String> target;
            if (name.startsWith("AVCProfile")) {
                target = sAVCProfiles;
            } else if (name.startsWith("AVCLevel")) {
                target = sAVCLevels;
            } else if (name.startsWith("AACObject")) {
                target = sAACProfiles;
            } else {
                continue;
            }
            try {
                target.put(f.getInt(null), name);
            } catch (IllegalAccessException e) {
                //ignored
            }
        }
    }

    static String toHumanReadable(int colorFormat) {
        if (sColorFormats.size() == 0) {
            initColorFormatFields();
        }
        int i = sColorFormats.indexOfKey(colorFormat);
        if (i >= 0) return sColorFormats.valueAt(i);
        return "0x" + Integer.toHexString(colorFormat);
    }

    /**
     * @param avcProfileLevel AVC CodecProfileLevel
     */
    static String avcProfileLevelToString(MediaCodecInfo.CodecProfileLevel avcProfileLevel) {
        if (sAVCProfiles.size() == 0 || sAVCLevels.size() == 0) {
            initProfileLevels();
        }
        String profile = null, level = null;
        int i = sAVCProfiles.indexOfKey(avcProfileLevel.profile);
        if (i >= 0) {
            profile = sAVCProfiles.valueAt(i);
        }

        i = sAVCLevels.indexOfKey(avcProfileLevel.level);
        if (i >= 0) {
            level = sAVCLevels.valueAt(i);
        }

        if (profile == null) {
            profile = String.valueOf(avcProfileLevel.profile);
        }
        if (level == null) {
            level = String.valueOf(avcProfileLevel.level);
        }
        return profile + '-' + level;
    }

    static String[] aacProfiles() {
        if (sAACProfiles.size() == 0) {
            initProfileLevels();
        }
        String[] profiles = new String[sAACProfiles.size()];
        for (int i = 0; i < sAACProfiles.size(); i++) {
            profiles[i] = sAACProfiles.valueAt(i);
        }
        return profiles;
    }

    static MediaCodecInfo.CodecProfileLevel toProfileLevel(String str) {
        if (sAVCProfiles.size() == 0 || sAVCLevels.size() == 0 || sAACProfiles.size() == 0) {
            initProfileLevels();
        }
        String profile = str;
        String level = null;
        int i = str.indexOf('-');
        if (i > 0) { // AVC profile has level
            profile = str.substring(0, i);
            level = str.substring(i + 1);
        }

        MediaCodecInfo.CodecProfileLevel res = new MediaCodecInfo.CodecProfileLevel();
        if (profile.startsWith("AVC")) {
            res.profile = keyOfValue(sAVCProfiles, profile);
        } else if (profile.startsWith("AAC")) {
            res.profile = keyOfValue(sAACProfiles, profile);
        } else {
            try {
                res.profile = Integer.parseInt(profile);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (level != null) {
            if (level.startsWith("AVC")) {
                res.level = keyOfValue(sAVCLevels, level);
            } else {
                try {
                    res.level = Integer.parseInt(level);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return res.profile > 0 && res.level >= 0 ? res : null;
    }

    private static <T> int keyOfValue(SparseArray<T> array, T value) {
        int size = array.size();
        for (int i = 0; i < size; i++) {
            T t = array.valueAt(i);
            if (t == value || t.equals(value)) {
                return array.keyAt(i);
            }
        }
        return -1;
    }

    //将音频解码器的能力打印出来
//    public static Observable<String> printAudioCodecCap(List<MediaCodecInfo> mediaCodecInfos, String mimeType) {
//        return Observable
//                .just(mediaCodecInfos)
//                .map(infoList -> {
//                    StringBuilder sb = new StringBuilder();
//                    sb.append("MIMETYPE_AUDIO_AAC INFO:")
//                            .append("\n").append("Size of infoList ")
//                            .append(infoList.size())
//                            .append("\n");
//                    for (MediaCodecInfo it : infoList) {
//                        sb.append(it.getName()).append("\n");
//                        //获取编码器的能力
//                        MediaCodecInfo.CodecCapabilities codecCapabilities = it.getCapabilitiesForType(mimeType);
//                        MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
//                        MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
//
//                        printVideoCap(sb, videoCapabilities);
//                        printColorFormat(sb, codecCapabilities);
//
//
//                        printAudioCap(sb, audioCapabilities);
//                    }
//                    return sb.toString();
//                });
//    }

    private static void printColorFormat(StringBuilder sb, MediaCodecInfo.CodecCapabilities codecCapabilities) {
        if (codecCapabilities == null) {
            return;
        }
        //colorFormats & profileLevels
        sb.append("\t").append("colorFormats INFO:").append("\n");
        int[] colorFormats = codecCapabilities.colorFormats;
        for (int colorFormat : colorFormats) {
            sb.append("\t").append("colorFormat : ").append(MediaCodecLogHelper.toHumanReadable(colorFormat)).append("\n");
        }
    }

    //    //将视频解码器的能力打印出来
//    public static Observable<String> printVideoCodecCap(List<MediaCodecInfo> mediaCodecInfos, String mimeType) {
//        return Observable
//                .just(mediaCodecInfos)
//                .map(infoList -> {
//                    StringBuilder sb = new StringBuilder();
//
//                    sb.append("MIME_TYPE_VIDEO_AVC INFO:")
//                            .append("\n").append("Size of infoList ")
//                            .append(infoList.size())
//                            .append("\n");
//
//                    for (MediaCodecInfo it : infoList) {
//                        sb.append(it.getName()).append("\n");
//                        //获取编码器的能力
//                        MediaCodecInfo.CodecCapabilities codecCapabilities = it.getCapabilitiesForType(mimeType);
//                        MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
//                        MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
//                        printVideoCap(sb, videoCapabilities);
//
//                        printProfileLevel(sb, codecCapabilities);
//                        //colorFormats & profileLevels
//                        printColorFormat(sb, codecCapabilities);
//
//                        printAudioCap(sb, audioCapabilities);
//
//                    }
//                    return sb.toString();
//                });
//    }
//    //将视频解码器的能力打印出来
    public static String printVideoCodecCap(List<MediaCodecInfo> infoList, String mimeType) {
        StringBuilder sb = new StringBuilder();

        sb.append("MIME_TYPE_VIDEO_AVC INFO:")
                .append("\n").append("Size of infoList ")
                .append(infoList.size())
                .append("\n");

        for (MediaCodecInfo it : infoList) {
            sb.append(it.getName()).append("\n");
            //获取编码器的能力
            MediaCodecInfo.CodecCapabilities codecCapabilities = it.getCapabilitiesForType(mimeType);
            MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
            MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
            printVideoCap(sb, videoCapabilities);

            printProfileLevel(sb, codecCapabilities);
            //colorFormats & profileLevels
            printColorFormat(sb, codecCapabilities);

            printAudioCap(sb, audioCapabilities);

        }
        return sb.toString();
    }

    private static void printProfileLevel(StringBuilder sb, MediaCodecInfo.CodecCapabilities codecCapabilities) {
        if (codecCapabilities == null) {
            return;
        }
        //profileLevels
        sb.append("\t").append("profileLevels INFO:").append("\n");
        MediaCodecInfo.CodecProfileLevel[] profileLevels = codecCapabilities.profileLevels;
        for (MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
            sb.append("\t").append("profileLevel : ").append(MediaCodecLogHelper.avcProfileLevelToString(profileLevel)).append("\n");
        }
    }

    /**
     * 音频编码器的能力
     * bitrate
     * maxInoutChannelCount
     * SampleRate
     */
    private static void printAudioCap(StringBuilder sb, MediaCodecInfo.AudioCapabilities audioCapabilities) {
        if (audioCapabilities == null) {
            return;
        }
        sb.append("\t").append("AudioCap INFO:").append("\n");
        sb.append("\t").append("bitrateRange=").append(audioCapabilities.getBitrateRange()).append("\n");
        sb.append("\t").append("maxInputChannelCount=").append(audioCapabilities.getMaxInputChannelCount()).append("\n");
        sb.append("\t").append("supportedSampleRateRanges=").append(Arrays.toString(audioCapabilities.getSupportedSampleRates())).append("\n");
        sb.append("\t").append("audioCapabilities=").append(audioCapabilities.toString()).append("\n");
    }

    /**
     * 视频编码器的能力包括
     * bitrate
     * FrameRate
     * Height
     * Width
     */
    private static void printVideoCap(StringBuilder sb, MediaCodecInfo.VideoCapabilities videoCapabilities) {
        if (videoCapabilities == null) {
            return;
        }
        sb.append("\t").append("VideoCap INFO:").append("\n");

        sb.append("\t").append("bitrateRange=").append(videoCapabilities.getBitrateRange()).append("\n");
        sb.append("\t").append("supportedFrameRates=").append(videoCapabilities.getBitrateRange()).append("\n");
        sb.append("\t").append("supportedHeights=").append(videoCapabilities.getSupportedHeights()).append("\n");
        sb.append("\t").append("supportedWidths=").append(videoCapabilities.getSupportedWidths()).append("\n");
        sb.append("\t").append("heightAlignment=").append(videoCapabilities.getHeightAlignment()).append("\n");
        sb.append("\t").append("widthAlignment=").append(videoCapabilities.getWidthAlignment()).append("\n");
    }


}
