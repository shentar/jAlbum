# jAlbum

==

这是一个使用Java语言编写的本地照片管理系统。使用BS架构。服务端采用Servlet提供RESTful风格接口，供浏览器直接访问。服务端提供后台任务扫描指定目录，并收集指定后缀名的照片文件，生成照片库，识别照片HASH指纹、长宽比、拍摄时间等，最终按照拍摄时间生成年、月、日的归档页面。对于重复照片只会显示一份。

This is a local photo management system written with the Java language. Using B/S architecture. Server Servlet provides a RESTful style interface, for direct access to the web browser. Service provides a background task scans the specified directory, and collect the specified suffix photos, then generate a photo gallery. HASH fingerprint is used to recognize the duplicate pic fils. The shooting time in Exif is used to sort all the files. By identifying the picture's aspect ratio, set the appropriate display size on a Web page. Eventually in time axis to generate a year, month, and day dimensions page.

## install
>### 0. For ARM platform, such as Raspberry3 ubuntu-mate system, compile jdbcsqlite native so first. Other platform may not need this.
cd jdbcsqlitenative<br/>
make

>### 1. Build start.jar
cd dedup
<br/>
ant 

>### 2. Build root.war
cd photoweb<br/>
sh build.sh


## start
>### 0. For the Raspberry3 ubuntu-mate system
cd jAlbum<br/>
sh start_for_Raspberry3.sh
<br/><br/>
-Dinputdir=/media/Ent/<br/>
-Dthreadcount=4<br/>
-Dhashalog=MD5<br/>
-Dinputdir specify the folder which nedd to scan.<br/>
-Dthreadcount specify the size of thread pool.<br/>
-Dhashalog specify the file HASH fingerprint Algorithm.<br/>
<br/>

>### 1. For other platforms 
cd dedup<br/>
java -Dinputdir=/media/Ent/ -Dthreadcount=64 -Dhashalog=SHA-256 -jar start.jar

## access
Open the url: http://ip:2148/ in any Web Browser.





