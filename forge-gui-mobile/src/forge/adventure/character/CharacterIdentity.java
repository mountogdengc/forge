package forge.adventure.character;

import forge.adventure.data.CharacterCreationData.Portrait;
import forge.adventure.util.SaveFileData;

/** Additive save section. Its absence deliberately keeps the original hero-index behavior. */
public final class CharacterIdentity {
    public String genderId = "", genderLabel = "", raceId = "", raceLabel = "";
    public String appearanceId = "", appearanceSprite = "", starterClassId = "";
    public Portrait portrait;
    public byte[] portraitPng = new byte[0];

    public SaveFileData toSaveData() {
        SaveFileData data = new SaveFileData();
        data.store("version", 1);
        data.store("genderId", genderId); data.store("genderLabel", genderLabel);
        data.store("raceId", raceId); data.store("raceLabel", raceLabel);
        data.store("appearanceId", appearanceId); data.store("appearanceSprite", appearanceSprite);
        data.store("starterClassId", starterClassId);
        if (portrait != null) data.store("portrait", new com.badlogic.gdx.utils.Json().toJson(portrait));
        if (portraitPng != null && portraitPng.length > 0) {
            if (portraitPng.length > PortraitImages.MAX_BYTES) throw new IllegalArgumentException("Portrait is too large to save.");
            data.put("portraitPng", portraitPng.clone());
        }
        return data;
    }

    public static CharacterIdentity fromSaveData(SaveFileData data) {
        if (data == null || data.readInt("version") != 1) return null;
        CharacterIdentity identity = new CharacterIdentity();
        identity.genderId = text(data, "genderId"); identity.genderLabel = text(data, "genderLabel");
        identity.raceId = text(data, "raceId"); identity.raceLabel = text(data, "raceLabel");
        identity.appearanceId = text(data, "appearanceId"); identity.appearanceSprite = text(data, "appearanceSprite");
        identity.starterClassId = text(data, "starterClassId");
        String portraitJson = data.readString("portrait");
        if (portraitJson != null) identity.portrait = new com.badlogic.gdx.utils.Json().fromJson(Portrait.class, portraitJson);
        byte[] image = data.get("portraitPng");
        if (image != null && image.length <= PortraitImages.MAX_BYTES) identity.portraitPng = image.clone();
        return identity;
    }

    private static String text(SaveFileData data, String key) {
        String value = data.readString(key);
        return value == null ? "" : value;
    }
}
