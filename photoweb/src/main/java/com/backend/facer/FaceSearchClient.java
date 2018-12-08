package com.backend.facer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceSearchClient
{
    private static final String RETURN_RESULT_COUNT = "return_result_count";

    private static final int MAX_RETURN_COUNT = 5;

    public static List<Face> searchFaces(String facetoken)
    {
        if (StringUtils.isBlank(facetoken))
        {
            return null;
        }

        String maxID = FaceSetToken.getInstance().getCurrentSN();
        if (StringUtils.isBlank(maxID))
        {
            return null;
        }

        int maxIDu = Integer.parseInt(maxID);

        if (maxIDu < 0)
        {
            return null;
        }

        if (maxIDu > 1000)
        {
            maxIDu = 1000;
        }

        List<Face> flst = new ArrayList<>();
        for (int i = 0; i <= maxIDu; i++)
        {
            Map<String, Object> mp = new HashMap<>();
            mp.put(FacerUtils.FACE_TOKEN, facetoken);
            mp.put(FacerUtils.OUTER_ID, FaceSetToken.getInstance().getFaceSetIDBySn(i));
            mp.put(RETURN_RESULT_COUNT, MAX_RETURN_COUNT + "");
            String result = FacerUtils.post(FacerUtils.SEARCH_URL, mp);

            List<Face> f = parseResult(result, facetoken);

            if (f != null && !f.isEmpty())
            {
                flst.addAll(f);
            }
        }

        return flst;
    }

    private static List<Face> parseResult(String result, String facetoken)
    {
        if (StringUtils.isBlank(result))
        {
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonObject jr = (JsonObject) parser.parse(result);
        JsonArray ja = jr.getAsJsonArray("results");
        if (ja != null && ja.size() > 0)
        {
            List<Face> ls = new ArrayList<>();
            for (int i = 0; i != ja.size(); i++)
            {
                JsonObject je = ja.get(i).getAsJsonObject();
                String token = je.get(FacerUtils.FACE_TOKEN).getAsString();

                if (StringUtils.isNotBlank(token) && !StringUtils.equals(facetoken, token))
                {
                    Face f = new Face();
                    f.setFacetoken(token);
                    JsonElement joe = je.get("confidence");
                    if (joe != null && StringUtils.isNotBlank(joe.getAsString()))
                    {
                        if ("90".compareTo(joe.getAsString()) <= 0)
                        {
                            ls.add(f);
                        }
                    }
                }
            }

            return ls;
        }

        return null;
    }
}
