package com.demo.chat.model.small;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.sticker.InputStickerSet;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/26
 * @description null
 * @usage null
 */
public class Document extends Media {
    public int flags;
    public long id;
    public long access_hash;
    public byte[] file_reference;
    public int user_id;
    public int date;
    public String file_name;
    public String mime_type;
    public int size;
    public int version;
    public int dc_id;
    public byte[] key;
    public byte[] iv;
    public ArrayList<PhotoSize> thumbs = new ArrayList<>();
    public ArrayList<VideoSize> video_thumbs = new ArrayList<>();
    public ArrayList<DocumentAttribute> attributes = new ArrayList<>();
    public static class MaskCoords{

        public int n;
        public double x;
        public double y;
        public double zoom;
    }
    public static class DocumentAttribute {
        public InputStickerSet stickerset;
        public MaskCoords mask_coords;
        public String alt;
        public int duration;
        public int flags;
        public boolean round_message;
        public boolean supports_streaming;
        public String file_name;
        public int w;
        public int h;
        public boolean mask;
        public String title;
        public String performer;
        public boolean voice;
        public byte[] waveform;

        public void setImageSize(boolean isImageSize) {}

        public void setVideo(boolean isVideo) {}

        public void setAudio(boolean isAudio) {}

        public void setSticker(boolean isSticker) {}

        public void setAnimated(boolean isAnimated) {}

        public boolean isImageSize() {
            return false;
        }

        public boolean isVideo() {
            return false;
        }

        public boolean isAudio() {
            return false;
        }

        public boolean isSticker() {
            return false;
        }

        public boolean isAnimated() {
            return false;
        }
        public boolean isFilename() {
            return false;
        }

        public void setFilename(boolean b) {

        }
    }

    public static Document TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        Document result = new Document();
        return result;
    }
}
