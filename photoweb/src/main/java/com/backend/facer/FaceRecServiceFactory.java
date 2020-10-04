package com.backend.facer;

import com.utils.conf.AppConfig;

public class FaceRecServiceFactory {
    public static FaceRecServiceInterface getFaceRecService() {
        if (AppConfig.getInstance().getFacerServiceType() == 0) {
            return FaceRecService.getInstance();
        } else {
            return LocalFaceRecService.getInstance();
        }
    }
}
