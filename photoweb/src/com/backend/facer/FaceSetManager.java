package com.backend.facer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceSetManager
{
    private static final Logger logger = LoggerFactory.getLogger(FaceSetManager.class);

    private static FaceSetManager instance = new FaceSetManager();

    private FaceSetManager()
    {

    }

    public static FaceSetManager getInstance()
    {
        return instance;
    }

    public boolean addFaceToSet(String faceToken)
    {
        if (StringUtils.isBlank(faceToken))
        {
            return true;
        }

        String facesetid = FaceSetToken.getInstance().acquireFaceSetID();
        Map<String, Object> mp = new HashMap<String, Object>();
        mp.put("face_tokens", faceToken);
        mp.put("outer_id", facesetid);
        int retryTimes = 3;
        while (retryTimes-- > 0)
        {
            String addResult = FacerUtils.post(FacerUtils.FACESET_ADD_URL, mp);
            if (StringUtils.isBlank(addResult))
            {
                // retry;
                logger.warn("add failed: [{}:{}]", facesetid, faceToken);
            }
            else
            {
                logger.warn("added the faceToken to faceset successfully: " + addResult);
                return true;
            }
        }

        return false;
    }

}
