package com.backend.facer;

import com.backend.entity.FileInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceDetectClient
{
    private static final Logger logger = LoggerFactory.getLogger(FaceDetectClient.class);

    public static List<Face> detectFace(FileInfo fi)
    {
        if (fi == null)
        {
            return null;
        }

        Map<String, Object> mp = new HashMap<>();
        Object file = FacerUtils.getFileForDetectFaces(fi);
        if (file == null)
        {
            logger.warn("can not get the file now.");
            return null;
        }

        mp.put(FacerUtils.IMG_FILE, file);
        mp.put("return_attributes", "gender,age,facequality");
        String result = FacerUtils.post(FacerUtils.DETACT_URL, mp);

        return parseDetectResult(fi, result);
    }

    private static List<Face> parseDetectResult(FileInfo fi, String result)
    {
        if (StringUtils.isBlank(result))
        {
            return null;
        }

        logger.warn("detect the file: {} \n{}", fi, result);
        JsonParser parser = new JsonParser();
        JsonObject jr = (JsonObject) parser.parse(result);
        JsonArray ja = jr.getAsJsonArray("faces");
        if (ja == null || ja.size() == 0)
        {
            return null;
        }

        List<Face> ls = new ArrayList<>();
        for (int i = 0; i != ja.size(); i++)
        {
            JsonObject je = ja.get(i).getAsJsonObject();
            String token = je.get(FacerUtils.FACE_TOKEN).getAsString();

            if (StringUtils.isNotBlank(token))
            {
                Face face = new Face();
                face.setFi(fi);
                face.setEtag(fi.getHash256());
                face.setFacetoken(token);

                JsonObject jo = je.getAsJsonObject("face_rectangle");
                if (jo != null)
                {
                    face.setPos(String.format("%d,%d,%d,%d", jo.get("width").getAsInt(),
                            jo.get("height").getAsInt(), jo.get("left").getAsInt(),
                            jo.get("top").getAsInt()));
                }

                jo = je.getAsJsonObject("attributes");
                if (jo != null)
                {
                    JsonObject age = jo.getAsJsonObject("age");
                    if (age != null)
                    {
                        face.setAge(age.get("value").getAsString());
                    }

                    JsonObject facequality = jo.getAsJsonObject("facequality");
                    if (facequality != null)
                    {
                        face.setQuality(facequality.get("value").getAsString());
                    }
                    else
                    {
                        face.setQuality("0");
                    }

                    JsonObject gender = jo.getAsJsonObject("gender");
                    if (gender != null)
                    {
                        face.setGender(gender.get("value").getAsString());
                    }
                }
                else
                {
                    face.setQuality("0");
                }

                ls.add(face);
            }
        }
        return ls;
    }
}
