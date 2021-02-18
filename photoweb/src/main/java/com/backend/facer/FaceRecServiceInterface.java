package com.backend.facer;

import com.backend.entity.FileInfo;

import java.util.List;

public interface FaceRecServiceInterface {
    void detectFaces(final FileInfo fi);

    void checkAllFacesID();

    void checkFaces(List<Face> lst);

    void checkOneFace(Face f);

    List<Face> getSortedFaces(long fid, int count, boolean needFileInfo);

    List<Face> checkAndGetFaceidList();

    boolean deleteOneFile(String id);

    boolean deleteOneFaceId(long id);
}
