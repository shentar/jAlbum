package com.backend;

import java.util.List;

public class VideoRecordsStore extends AbstractRecordsStore
{
    // private static final Logger logger =
    // LoggerFactory.getLogger(UniqPhotosStore.class);

    private static VideoRecordsStore instance = new VideoRecordsStore();

    private VideoRecordsStore()
    {

    }

    public static VideoRecordsStore getInstance()
    {
        return instance;
    }

    public List<VideoFile> getAllVideos()
    {
        return null;
    }

    public void insertOneVideo(VideoFile vf)
    {
    }

    public boolean checkVideoExist(VideoFile vf)
    {
        return false;
    }

    public VideoFile getOneVideoByID(String id)
    {
        return null;
    }

    public List<VideoFile> getNextPage(String id, int count, boolean isNext)
    {
        return null;
    }

    public void deleteRecordByID(String id)
    {

    }
}
