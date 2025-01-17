package com.demo.chat.model.sticker;

import com.demo.chat.messager.AbstractSerializedData;
import com.demo.chat.model.small.Document;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class MessagesStickerSet extends Sticker{
    public StickerSet set;
    public ArrayList<StickerPack> packs = new ArrayList<>();
    public ArrayList<Document> documents = new ArrayList<>();
    public static MessagesStickerSet TLdeserialize(AbstractSerializedData stream, int constructor, boolean exception) {
        MessagesStickerSet result = new MessagesStickerSet();
        return result;
    }
    public static class StickerPack{
        public String emoticon;
        public ArrayList<Long> documents = new ArrayList<>();
    }
}
