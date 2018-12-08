package com.backend.facer;

import com.backend.dao.GlobalConfDao;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.conf.AppConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FaceSetToken
{
    public static final String CURRENT_FACESETID_CONF_KEY = "facesetkeyid";

    public static final String FACESET_ID_PREFIX = AppConfig.getInstance().getFaceSetPrefix();

    private static final Logger logger = LoggerFactory.getLogger(FaceSetToken.class);

    private int currentFaceSetID = -1;

    private static FaceSetToken instance = new FaceSetToken();

    private int facecount = -1;

    private volatile boolean isInit = false;

    private FaceSetToken()
    {

    }

    public static FaceSetToken getInstance()
    {
        instance.init();
        return instance;
    }

    public synchronized void init()
    {
        if (isInit)
        {
            return;
        }

        try
        {
            if (currentFaceSetID < 0)
            {
                String cid = getLastSNFromConfTable();
                if (!StringUtils.isBlank(cid))
                {
                    try
                    {
                        currentFaceSetID = Integer.parseInt(cid);
                    }
                    catch (Exception e)
                    {
                        logger.warn("fomat error: ", e);
                    }
                }
                else
                {
                    currentFaceSetID = 0;
                    GlobalConfDao.getInstance()
                            .setConf(CURRENT_FACESETID_CONF_KEY, currentFaceSetID + "");
                }
            }

            if (facecount < 0)
            {
                facecount = getFaceCount();
                logger.warn("the count of faces in the faceset[{}] is {}", getCurrentFaceSetID(),
                            facecount);
            }

            while (facecount >= 1000)
            {
                refreshFaceCount();
            }

            logger.warn("init successfully, the current faceset is " + getCurrentFaceSetID());

            isInit = true;
        }
        catch (Exception e)
        {
            logger.warn("caused by: ", e);
            // TODO need sleep a time.
        }
    }

    private void refreshFaceCount()
    {
        currentFaceSetID++;
        GlobalConfDao.getInstance().setConf(CURRENT_FACESETID_CONF_KEY, currentFaceSetID + "");
        facecount = getFaceCount();
        logger.warn("the faceset id is {}, facecount is {}", currentFaceSetID, facecount);
    }

    public synchronized String acquireFaceSetID()
    {
        while (facecount >= 1000)
        {
            refreshFaceCount();
        }

        facecount++;
        return getCurrentFaceSetID();
    }

    /*
     * 必选 api_key String 调用此API的API Key 必选 api_secret String 调用此API的API Secret
     * 可选 display_name String 人脸集合的名字，最长256个字符，不能包括字符^@,&=*'" 可选 outer_id String
     * 账号下全局唯一的FaceSet自定义标识，可以用来管理FaceSet对象。最长255个字符，不能包括字符^@,&=*'" 可选 tags
     * String
     * FaceSet自定义标签组成的字符串，用来对FaceSet分组。最长255个字符，多个tag用逗号分隔，每个tag不能包括字符^@,&=*'"
     * 可选 face_tokens String 人脸标识face_token，可以是一个或者多个，用逗号分隔。最多不超过5个face_token 可选
     * user_data String 自定义用户信息，不大于16KB，不能包括字符^@,&=*'" 可选 force_merge Int
     * 在传入outer_id的情况下，如果outer_id已经存在，是否将face_token加入已经存在的FaceSet中
     * 0：不将face_tokens加入已存在的FaceSet中，直接返回FACESET_EXIST错误
     * 1：将face_tokens加入已存在的FaceSet中 默认值为0
     */
    private void createFaceSet()
    {
        String facesetid = getCurrentFaceSetID();
        Map<String, Object> mp = new HashMap<>();
        mp.put("display_name", "jAlbum_FaceSet");
        mp.put("outer_id", facesetid);
        String result = FacerUtils.post(FacerUtils.FACESET_CREATE_URL, mp);
        if (StringUtils.isBlank(result))
        {
            logger.warn("create faceset failed: " + facesetid);
        }
        else
        {
            logger.warn("created the faceset: ", result);
            GlobalConfDao.getInstance().setConf(CURRENT_FACESETID_CONF_KEY, currentFaceSetID + "");
        }
    }

    private int getFaceCount()
    {
        List<String> flst = getFaceTokens(getCurrentFaceSetID());
        if (flst == null)
        {
            createFaceSet();
            return 0;
        }

        return flst.size();
    }

    public List<String> getFaceTokens(String faceSetID)
    {
        if (StringUtils.isBlank(faceSetID))
        {
            return null;
        }

        Map<String, Object> mp = new HashMap<>();
        mp.put(FacerUtils.OUTER_ID, faceSetID);
        String result = FacerUtils.post(FacerUtils.FACESET_DETAIL_URL, mp);

        if (StringUtils.isBlank(result))
        {
            return null;
        }

        List<String> flst = new LinkedList<>();

        JsonParser parser = new JsonParser();
        JsonObject jr = (JsonObject) parser.parse(result);
        JsonArray ja = jr.getAsJsonArray("face_tokens");
        if (ja != null)
        {
            for (int i = 0; i != ja.size(); i++)
            {
                flst.add(ja.get(i).getAsString());
            }
        }

        return flst;
    }

    private String getLastSNFromConfTable()
    {
        return GlobalConfDao.getInstance().getConf(CURRENT_FACESETID_CONF_KEY);
    }

    public String getCurrentSN()
    {
        if (currentFaceSetID == -1)
        {
            return getLastSNFromConfTable();
        }

        return currentFaceSetID + "";
    }

    private String getCurrentFaceSetID()
    {
        return getFaceSetIDBySn(currentFaceSetID);
    }

    public String getFaceSetIDBySn(int sn)
    {
        return FACESET_ID_PREFIX + sn;
    }
}
