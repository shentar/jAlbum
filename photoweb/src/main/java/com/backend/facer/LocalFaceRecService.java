package com.backend.facer;

import com.backend.dao.FaceTableDao;
import com.backend.entity.FileInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utils.conf.AppConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalFaceRecService extends FaceRecService implements FaceRecServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(LocalFaceRecService.class);

    private static class ServiceHolder {
        private static final FaceRecServiceInterface instance = new LocalFaceRecService();
    }

    public static FaceRecServiceInterface getInstance() {
        return LocalFaceRecService.ServiceHolder.instance;
    }

    @Override
    public void detectOnePic(FileInfo fi) {
        CloseableHttpResponse response = null;
        try {
            String url = String.format("http://%s:%d/api/detect",
                    AppConfig.getInstance().getFacerEndPoint(),
                    AppConfig.getInstance().getFacerPort());
            HttpPut req = new HttpPut(url);
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new FileInputStream(new File(fi.getPath())));
            req.setEntity(entity);
            FaceClient.config(req);

            response = FaceClient.getHttpClient().execute(req);

            HttpEntity body = response.getEntity();
            String result = EntityUtils.toString(body, "utf-8");
            EntityUtils.consume(body);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                logger.error("detect faces failed: " + result);
                return;
            }

            logger.warn("detect the file: {} \n{}", fi, result);
            JsonParser parser = new JsonParser();
            JsonObject jr = (JsonObject) parser.parse(result);
            String ret = jr.get("success").getAsString();
            if (!StringUtils.equals(ret, "true")) {
                logger.error("detect faces failed: " + result);
                return;
            }

            int faceCount = jr.get("count").getAsInt();
            if (faceCount == 0) {
                FaceTableDao.getInstance().addEmptyRecords(fi);
                return;
            }

            JsonArray ja = jr.getAsJsonArray("faces");
            if (ja == null) {
                logger.error("detect faces failed: " + result);
                return;
            }

            List<Face> ls = new ArrayList<>();
            for (int i = 0; i != ja.size(); i++) {
                /*
                 *   "faces": [
                 *     {
                 *       "token": "5827851bb313f2bd9b0a43871bace91098c2d875c7e2914db8dd733dee419d51",
                 *       "rectangle": {
                 *         "width": 156,
                 *         "height": 156,
                 *         "left": 1336,
                 *         "top": 1130
                 *       },
                 *       "age": 0,
                 *       "quality": 0,
                 *       "gender": 0
                 *     },
                 *     {
                 *       "token": "ecd0af9d18dc94d71814df315cf3c23d4458688ce5589fdb15741ca8e40306d6",
                 *       "rectangle": {
                 *         "width": 75,
                 *         "height": 76,
                 *         "left": 1689,
                 *         "top": 992
                 *       },
                 *       "age": 0,
                 *       "quality": 0,
                 *       "gender": 0
                 *     }
                 *   ]
                 */
                JsonObject je = ja.get(i).getAsJsonObject();
                String token = je.get("token").getAsString();

                if (StringUtils.isBlank(token)) {
                    continue;
                }

                Face face = new Face();
                face.setFi(fi);
                face.setEtag(fi.getHash256());
                face.setFacetoken(token);

                JsonObject jo = je.getAsJsonObject("rectangle");
                if (jo != null) {
                    face.setPos(String.format("%d,%d,%d,%d", jo.get("width").getAsInt(),
                            jo.get("height").getAsInt(), jo.get("left").getAsInt(),
                            jo.get("top").getAsInt()));
                }

                face.setQuality("0");
                ls.add(face);
            }

            if (ls.isEmpty()) {
                FaceTableDao.getInstance().addEmptyRecords(fi);
                return;
            }

            FaceTableDao.getInstance().addRecords(ls);
            checkFaces(ls);
        } catch (Exception e) {
            logger.warn("some error, ", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.warn("some error, ", e);
                }
            }
            logger.warn("end to detect the file {}", fi);
        }
    }

    @Override
    public synchronized void checkOneFace(Face f) {
        CloseableHttpResponse response = null;
        try {
            String url = String.format("http://%s:%d/api/search?facetoken=%s&max-distance=%s",
                    AppConfig.getInstance().getFacerEndPoint(),
                    /*AppConfig.getInstance().getFacerPort()*/12547,
                    f.getFacetoken(),
                    "0.36");
            HttpPut req = new HttpPut(url);
            FaceClient.config(req);
            response = FaceClient.getHttpClient().execute(req);

            HttpEntity body = response.getEntity();
            String result = EntityUtils.toString(body, "utf-8");
            EntityUtils.consume(body);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                logger.error("search faces failed: " + result);
                return;
            }

            logger.debug("search the face: {} \n{}", f.getFacetoken(), result);
            JsonParser parser = new JsonParser();
            JsonObject jr = (JsonObject) parser.parse(result);
            String ret = jr.get("success").getAsString();
            if (!StringUtils.equals(ret, "true")) {
                logger.error("detect faces failed: " + result);
                return;
            }

            JsonArray ja = jr.getAsJsonArray("faces");
            if (ja == null) {
                logger.error("detect faces failed: " + result);
                return;
            }

            List<Face> rlst = new ArrayList<>();
            for (int i = 0; i != ja.size(); i++) {
                /*
                 * {
                 *   "success": true,
                 *   "count": 2,
                 *   "faces": [
                 *     {
                 *       "token_id": "015f13e4e55d04babb387ca1e56945fcfe867fb8b9203ee14fe3d6c1db46e139",
                 *       "distance": 0.3654655723266346
                 *     },
                 *     {
                 *       "token_id": "c78ece4c7fcf3855513639658156d0203c7b65141a2d36975de4ea2519b0e5f5",
                 *       "distance": 0.31392620349675987
                 *     }
                 *   ]
                 * }
                 */
                JsonObject je = ja.get(i).getAsJsonObject();
                String token = je.get("token_id").getAsString();
                float dis = je.get("distance").getAsFloat();
                Face fi = new Face();

                fi.setFacetoken(token);
                fi.setQuality((1 - dis) + "");
                rlst.add(fi);
            }

            if (rlst.isEmpty()) {
                logger.warn("search no faceid in the db. need acquire a new faiceid: {}", f);
                f.setFaceid(FaceIDManager.getInstance().acquireNewFaceID());
                FaceTableDao.getInstance().updateFaceID(f);
            } else {
                rlst.add(f);
                // FacerUtils.sortByQuality(rlst);
                logger.warn("searched {} faces in the db of {}.", rlst.size(), f);
                long faceid = -1;

                for (Face rf : rlst) {
                    Face fc = FaceTableDao.getInstance().getFace(rf.getFacetoken(), false);

                    if (fc == null) {
                        continue;
                    }

                    if (faceid == -1) {
                        faceid = fc.getFaceid();
                    }

                    // refresh existed faces' id to the fixed one.
                    if (fc.getFaceid() != -1 && faceid != -1) {
                        if (faceid != fc.getFaceid()) {
                            FaceTableDao.getInstance().updateFaceID(fc.getFaceid(), faceid);
                        }
                    }
                }

                if (faceid == -1) {
                    faceid = FaceIDManager.getInstance().acquireNewFaceID();
                }

                for (Face ft : rlst) {
                    ft.setFaceid(faceid);
                }

                FaceTableDao.getInstance().updateFaceID(rlst);
            }
        } catch (Exception e) {
            logger.warn("some error", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.warn("some error, ", e);
                }
            }
        }
    }
}
