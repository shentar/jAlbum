package com.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoRecordsStore extends AbstractRecordsStore
{
    private static final Logger logger = LoggerFactory.getLogger(UniqPhotosStore.class);

    private static VideoRecordsStore instance = new VideoRecordsStore();

    private VideoRecordsStore()
    {

    }

    public static VideoRecordsStore getInstance()
    {
        return instance;
    }

}
