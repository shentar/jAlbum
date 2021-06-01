# jAlbum

这是一个使用Java语言编写的本地照片管理系统。使用BS架构。服务端采用Servlet提供RESTful风格接口和动态页面供浏览器直接访问，集成照片Exif信息处理、视频流信息处理和人像识别等技术，集成本地和Amazon
S3云存储同步备份功能。服务端提供后台任务扫描指定目录，并收集指定后缀名的照片文件，生成照片库，识别照片HASH指纹、长宽比、拍摄时间等，最终按照拍摄时间生成年、月、日、人像归集的归档页面，并根据配置信息将数据保存到远端云存储上面（S3）。对于重复照片只会显示一份。相册界面可自适应兼容桌面操作系统浏览器和移动操作系统浏览器。项目主页：[主页](https://codefine.site/2837.html)。

This is a local photo management system written with the Java language. Using B/S architecture. The server uses Servlet
to provide RESTful style interface and dynamic page for direct access to the web browser, integrated photos Exif
information processing, video stream information processing and face recognition technology, integrated local and Amazon
S3 cloud storage synchronous backup function. Service provides a background task scans the specified directory, and
collect the specified suffix photos, then generate a photo gallery. HASH fingerprint is used to recognize the duplicate
pic fils. The shooting time in Exif is used to sort all the files. By identifying the picture's aspect ratio, set the
appropriate display size on a Web page. Eventually in time axis to generate a year, month, and day dimensions page.
HomePage of jAlbum：[HomePage](https://codefine.site/2837.html)。
