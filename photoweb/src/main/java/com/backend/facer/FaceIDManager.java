package com.backend.facer;

import com.backend.dao.GlobalConfDao;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceIDManager
{
    private static final Logger logger = LoggerFactory.getLogger(FaceIDManager.class);

    private static FaceIDManager instance = new FaceIDManager();

    private static final String FACEID_CONF_STR = "maxfaceid";

    private long faceid = -1;

    private FaceIDManager()
    {

    }

    public static FaceIDManager getInstance()
    {
        return instance;
    }

    public synchronized long acquireNewFaceID()
    {
        if (faceid == -1)
        {
            String maxID = GlobalConfDao.getInstance().getConf(FACEID_CONF_STR);
            if (!StringUtils.isBlank(maxID))
            {
                faceid = Long.parseLong(maxID);
            }
        }

        faceid++;
        GlobalConfDao.getInstance().setConf(FACEID_CONF_STR, faceid + "");

        long fidtmp = faceid;
        logger.warn("alloc a new faceid: {}", fidtmp);

        return fidtmp;
    }
}
