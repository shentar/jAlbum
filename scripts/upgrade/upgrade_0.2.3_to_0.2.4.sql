PRAGMA foreign_keys = 0;

CREATE TABLE sqlitestudio_temp_table AS SELECT *
                                          FROM faces;

DROP TABLE faces;

CREATE TABLE faces (
    facetoken STRING,
    etag      STRING (32, 32),
    pos       STRING,
    faceid    BIGINT,
    quality   STRING,
    gender    STRING,
    age       STRING,
    ptime     DATE
);

INSERT INTO faces (
                      facetoken,
                      etag,
                      pos,
                      faceid,
                      quality,
                      gender,
                      age
                  )
                  SELECT facetoken,
                         etag,
                         pos,
                         faceid,
                         quality,
                         gender,
                         age
                    FROM sqlitestudio_temp_table;

DROP TABLE sqlitestudio_temp_table;

CREATE INDEX faceindex ON faces (
    facetoken,
    etag,
    faceid,
    ptime
);

PRAGMA foreign_keys = 1;