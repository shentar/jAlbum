# jAlbum
[![Build Status](https://travis-ci.org/shentar/jAlbum.svg?branch=master)](https://travis-ci.org/shentar/jAlbum)
==

这是一个使用Java语言编写的本地照片管理系统。使用BS架构。服务端采用Servlet提供RESTful风格接口和动态页面供浏览器直接访问，集成照片Exif信息处理、视频流信息处理和人像识别等技术，集成本地和Amazon S3云存储同步备份功能。服务端提供后台任务扫描指定目录，并收集指定后缀名的照片文件，生成照片库，识别照片HASH指纹、长宽比、拍摄时间等，最终按照拍摄时间生成年、月、日、人像归集的归档页面，并根据配置信息将数据保存到远端云存储上面（S3）。对于重复照片只会显示一份。相册界面可自适应兼容桌面操作系统浏览器和移动操作系统浏览器。

This is a local photo management system written with the Java language. Using B/S architecture. The server uses Servlet to provide RESTful style interface and dynamic page for direct access to the web browser, integrated photos Exif information processing, video stream information processing and face recognition technology, integrated local and Amazon S3 cloud storage synchronous backup function. Service provides a background task scans the specified directory, and collect the specified suffix photos, then generate a photo gallery. HASH fingerprint is used to recognize the duplicate pic fils. The shooting time in Exif is used to sort all the files. By identifying the picture's aspect ratio, set the appropriate display size on a Web page. Eventually in time axis to generate a year, month, and day dimensions page.

## install
>### For ARM platform, such as Raspberry3 ubuntu-mate system, compile jdbcsqlite native so first. Other platform may not need this.
```shell
cd jAlbum
mvn clean compile package install -f pom_for_Raspberry3.xml
```
For other platforms, such as Windows, X86 Linux etc. just run the command like:
```shell
cd jAlbum
mvn clean compile package install 
```
The build target files are store in the jAlbum/distribute folder.

You can also get the release package in this web page: [jAlbum release](http://codefine.co/2837.html)

## start
>### 0. Configure the jalbum.xml file.
cd jAlbum/distribute<br/>
edit the jalbum.xml
<br/><br/>
```xml
<?xml version="1.0" encoding="utf-8" ?>  
<config>
    <picfilesuffix>
        <suffix>jpg</suffix>
        <suffix>jpeg</suffix>
        <suffix>png</suffix>
    </picfilesuffix>   
    <minfilesize>51200</minfilesize>
    <maxfilesize>512000000</maxfilesize>
    <threadcount>20</threadcount>
    <maxpicsperonepage>60</maxpicsperonepage>
    <hashalog>MD5</hashalog>
    <accessAuth>true</accessAuth>
    <inputdir>
        <dir>D:\\</dir>
        <dir>C:\\</dir>
    </inputdir>
    <excludedir>
        <dir>C:\\windows\\</dir>
        <dir>C:\\Program Files\\</dir>
        <dir>./</dir>
    </excludedir>
    <thumbnaildir>
        ./thumbnail
    </thumbnaildir>
    <Proxy>
        <host></host>
        <port></port>
        <user></user>
        <password></password>
    </Proxy>    
    <s3>
        <bucketname></bucketname>
        <ak></ak>
        <sk></sk>
        <useHttps>false</useHttps>
    </s3>
    <HuaweiOBS>
        <bucketname></bucketname>
        <ak></ak>
        <sk></sk>
        <EndPoint></EndPoint>
        <useHttps>false</useHttps>
    </HuaweiOBS>    
    <Facer>
        <ak></ak>
        <sk></sk>
        <facesetprefix>jalbum_faceset_id__</facesetprefix>
    </Facer>
</config>
```
***picfilesuffix*** the file type with the suffix that can be scaned by the tool. It is case-insensitive. If the "mp4" suffix is configured, You must make sure there is ffprobe exe command in the server. See [FFmpeg](http://www.ffmpeg.org/). for details.<br/>
***inputdir*** specify the folder which need to scan.<br/>
***excludedir*** specify the folder which you do not like the tool scan and display the content of it.<br/>
***minfilesize*** specify the min size of Pic file in byte.<br/>
***maxfilesize*** specify the max size of Pic file in byte.<br/>
***maxpicsperonepage*** specify the max pic count of one index page.</br>
***threadcount*** specify the size of thread pool.<br/>
***hashalog*** specify the file HASH fingerprint Algorithm. The common algorithms are: SHA-256, MD5, SHA-1. You can find the stand algorithm names in [MessageDigest Algorithms](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest).<br/>
***accessAuth*** specifie whether public access is allowed.<br/>
***thumbnaildir*** specify the folder which to store the thumbnail to.<br/>
***s3*** specify the Amazon S3 backup info such as bucketname, ak, sk and useHttps.<br/>
***HuaweiOBS*** specify the Huawei Cloud OBS backup info such as access endpoint, bucketname, ak, sk and useHttps.<br/>
***Facer*** specify the face++ service API info, such as ak, sk and custom facesetprefix.
<br/>

>### 1. Start the tool
For the ARM platforms like Raspberry3 ubuntu-mate system: <br/>
```shell
cd jAlbum/distribute
sh start_for_Raspberry3.sh
```
For other linux platforms: 
```shell
cd jAlbum/distribute
sh start.sh
```
For Windows platforms:
```
cd jAlbum/distribute
start.bat
```

## access
>###  
Open the url: http://ip:2148/ in any Web Browser to access the photo album.
After you enable access authentication, you need to get the token before you can access it. In the local host of jAlbum running in, visit the following address can manage the token:<br/>
1) http://127.0.0.1:2148/getToken, get the token generated by the server.<br/>
2) http://127.0.0.1:2148/clearToken, clear all the current token.<br/>
3) http://127.0.0.1:2148/removeToken?token=${token}, clear the specified token, and generate a new token.<br/>
<br/>
Login with superToken, allowing you to hide photos or videos, normal tokens can only view the album.<br/>

## screenshot
[jAlbum Screenshot](http://codefine.co/2837.html)

