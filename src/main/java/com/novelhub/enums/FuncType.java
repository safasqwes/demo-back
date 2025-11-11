package com.novelhub.enums;

/**
 * 功能类型（保留与历史数据兼容的编码）
 */
public enum FuncType {
    SYSTEM(0, "系统"),
    SINGLE_FACE(1, "单人换脸"),
    MULTI_FACE(2, "多人换脸"),
    BASIC_VIDEO(3, "基础视频换脸"),
    ADVANCED_VIDEO(4, "高级视频换脸"),
    GIF_FACE(5, "GIF换脸"),
    BATCH_FACE(6, "批量换脸"),
    EXTRACT_FACE(7, "拆脸获取点位"),
    BATCH_TARGET_FACE(8, "批量脸图换脸"),
    BATCH_IMAGE(9, "批量图片"),
    CHANGE_CLOTH_V1(10, "小工具换衣Remover"),
    UPSCALE_IMAGE(11, "小工具一键放大"),
    ENHANCE_IMAGE(12, "小工具图片增强"),
    ENHANCE_FACE(13, "小工具面部增强"),
    CHANGE_CLOTH(14, "小工具换衣Changer"),
    EXTRACT_MASK(15, "换衣蒙层"),
    HEADSHOT(16, "headshot换脸"),
    REMOVE_MARK(17, "移除水印"),
    AI_STICKER(18, "AI贴纸"),
    FACE_CUTOUT(19, "头贴"),
    CHANGE_HAIR(20, "换发"),
    AI_BODY_V1(21, "AI绘画"),
    TXT2VIDEO(22, "文字转视频"),
    IMG2VIDEO(23, "图片转视频"),
    SCENE2VIDEO(24, "场景图片转视频"),
    HEADTAIL2VIDEO(25, "首尾帧转视频"),
    CHARACTER2VIDEO(26, "参考生成视频"),
    ENLARGE_BREAST(27, "丰胸"),
    REMOVE_BG(28, "移除背景"),
    ENHANCE_VIDEO(29, "视频增强"),
    EXTEND_IMAGE(30, "扩图"),
    AI_TATTOO(31, "AI纹身"),
    REMOVE_VIDEO_MARK(32, "移除视频水印"),
    TALKING_PHOTO(33, "图片+音频 唇形同步视频"),
    TEXT2SPEECH(34, "文本转语音"),
    VOICE_CLONE(35, "声音克隆"),
    TEXT2IMAGE(36, "模型文生图"),
    IMAGE2IMAGE(37, "模型图生图"),
    IMAGE_EDITOR(38, "模型图片编辑"),
    TEXT2MUSIC(39, "文本转音乐"),
    REMOVE_VIDEO_BACKGROUND(40, "视频去背景"),
    AI_LIP_SYNC(41, "视频对口型"),
    VIDEO_AUTO_SUBTITLE(42, "视频增加字幕"),
    IMAGE_INPAINT(43, "图片重绘"),
    IMAGE_FILTER(44, "图片滤镜"),
    LIVE_PORTRAIT(45, "live portrait"),
    MEDIA2AUDIO(46, "社媒链接转音频"),
    VOICE_EXTRACT(47, "声音提取"),
    NANO_BANANA(48, "nanobanana"),
    AI_LOGO(49, "demo-ai-logo");

    private final int code;
    private final String description;

    FuncType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}


