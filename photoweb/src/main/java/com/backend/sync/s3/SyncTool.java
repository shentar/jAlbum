package com.backend.sync.s3;

import com.backend.entity.FileInfo;
import com.backend.threadpool.ThreadPoolFactory;
import com.utils.conf.AppConfig;
import com.utils.web.HeadUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class SyncTool {
    private static final Logger logger = LoggerFactory.getLogger(SyncTool.class);

    private static final ExecutorService threadPool =
            ThreadPoolFactory.getThreadPool(ThreadPoolFactory.SYNC_TOOL);

    public static void submitSyncTask(FileInfo fi) {
        if ((AppConfig.getInstance().isS3Configed() || AppConfig.getInstance()
                .isHuaweiOBSConfiged()) && new File(fi.getPath()).exists()) {
            threadPool.submit(new SyncS3Task(fi));
        }
    }

    public static void syncPhotoLibrary(final FileInfo fi) {
        String dir = AppConfig.getInstance().getPhotoLibraryDir();
        if (StringUtils.isBlank(dir)) {
            return;
        }
        SimpleDateFormat sf = new SimpleDateFormat("yyyy/MM/dd/");

        String libraryPath = dir +
                File.separator +
                sf.format(new Date(fi.getPhotoTime().getTime()));
        final String fullFilePath = libraryPath + fi.getHash256() + HeadUtils.getFileNameSuffix(fi.getFtype());

        final File f = new File(fullFilePath);
        if (f.exists()) {
            return;
        }

        File pDir = new File(libraryPath);
        if (!pDir.exists()) {
            boolean res = pDir.mkdirs();
            if (!res) {
                logger.warn("mkdirs failed: " + libraryPath);
            }
        }

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (fullFilePath.intern()) {
                    File origFile = new File(fi.getPath());
                    String dstTmp = fullFilePath + ".tmp";
                    File ftmp = new File(dstTmp);
                    try {
                        Files.copy(origFile.toPath(), ftmp.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                        logger.warn(String.format("Add one file to the library: %s -> %s",
                                origFile.getAbsolutePath(), ftmp.getAbsolutePath()));
                        boolean res = ftmp.renameTo(f);
                        if (!res) {
                            logger.warn(String.format("rename file failed: %s -> %s",
                                    ftmp.getAbsolutePath(), f.getAbsolutePath()));
                        }
                    } catch (Exception e) {
                        logger.warn(String.format("copy file failed: %s -> %s",
                                origFile.getAbsolutePath(), f.getAbsolutePath()));
                    }
                }
            }
        });
    }
}
