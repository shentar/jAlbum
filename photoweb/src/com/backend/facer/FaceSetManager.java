package com.backend.facer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dao.FaceTableDao;

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

    public boolean deleteFaceFromSet(List<String> flst, String faceSetID)
    {
        if (flst == null || flst.isEmpty() || StringUtils.isBlank(faceSetID))
        {
            return true;
        }

        Map<String, Object> mp = new HashMap<String, Object>();
        String fts = getFaceTokensString(flst);
        mp.put("face_tokens", fts);
        mp.put("outer_id", faceSetID);
        int retryTimes = 3;
        while (retryTimes-- > 0)
        {
            String addResult = FacerUtils.post(FacerUtils.FACESET_TOKEN_REMOVE_URL, mp);
            if (StringUtils.isBlank(addResult))
            {
                // retry;
                logger.warn("delete failed: [{}:{}]", faceSetID, fts);
            }
            else
            {
                logger.warn("delete the faceTokens [{}] from faceset [{}] successfully.", fts,
                        faceSetID);
                return true;
            }
        }

        return false;
    }

    private String getFaceTokensString(List<String> flst)
    {
        StringBuffer sb = new StringBuffer();
        for (String ftoken : flst)
        {
            sb.append(ftoken).append(",");
        }

        String res = sb.toString();
        if (res.endsWith(","))
        {
            return res.substring(0, res.length() - 1);
        }

        return null;
    }

    public synchronized void checkFaceSet()
    {
        String maxID = FaceSetToken.getInstance().getCurrentSN();
        if (StringUtils.isBlank(maxID))
        {
            return;
        }

        int maxIDu = Integer.parseInt(maxID);

        if (maxIDu < 0)
        {
            return;
        }

        if (maxIDu > 1000)
        {
            maxIDu = 1000;
        }

        for (int i = 0; i <= maxIDu; i++)
        {
            String faceSetID = FaceSetToken.getInstance().getFaceSetIDBySn(i);
            List<String> flst = FaceSetToken.getInstance().getFaceTokens(faceSetID);
            List<String> dlst = new LinkedList<String>();
            if (flst != null)
            {
                for (String token : flst)
                {
                    Face f = FaceTableDao.getInstance().getFace(token);
                    if (f == null)
                    {
                        dlst.add(token);
                    }
                }
            }

            deleteFaceFromSet(dlst, faceSetID);
        }
    }

}
