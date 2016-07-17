ALTER TABLE files RENAME TO sqlitestudio_temp_table;

CREATE TABLE files (path STRING PRIMARY KEY NOT NULL UNIQUE COLLATE BINARY, sha256 STRING (64, 64), size BIGINT, ctime DATE, phototime DATE, width BIGINT, height BIGINT, deleted STRING);

INSERT INTO files (path, sha256, size, ctime, phototime, width, height) SELECT path, sha256, size, ctime, phototime, width, height FROM sqlitestudio_temp_table;

DROP TABLE sqlitestudio_temp_table;

CREATE INDEX pathindex ON files (path ASC);