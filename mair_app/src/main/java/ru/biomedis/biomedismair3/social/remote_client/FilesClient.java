package ru.biomedis.biomedismair3.social.remote_client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.form.FormData;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import ru.biomedis.biomedismair3.social.remote_client.dto.DirectoryData;
import ru.biomedis.biomedismair3.social.remote_client.dto.FileData;
import ru.biomedis.biomedismair3.social.remote_client.dto.FileType;
import ru.biomedis.biomedismair3.social.remote_client.dto.UserNameDto;

public interface FilesClient {

  @RequestLine("POST /uploadFile/to_directory/{directory}")
  @Headers("Content-Type: multipart/form-data")
  FileData uploadFile(@Param("file") FormData file, @Param("directory") long directory);


  @RequestLine("GET /file/{id}")
  byte[] downloadFile(@Param("id") long id);

  @RequestLine("GET /file/{id}/thumbnail")
  byte[] downloadThumbnailFile(@Param("id") long id);

  @RequestLine("GET /directories/{directory}")
  List<DirectoryData> getDirectories(@Param("directory") long directory);

  @RequestLine("GET /directories/0")
  List<DirectoryData> getRootDirectories();

  @RequestLine("GET /directory/{directory}/files?type={type}")
  List<FileData> getFiles(@Param("directory") long directory, @Param("type") FileType type);

  @RequestLine("GET /directory/0/files")
  List<FileData> getRootFiles();

  @RequestLine("GET /directory/{directory}/files")
  List<FileData> getFilesAllType(@Param("directory") long directory);

  @RequestLine("PUT /directory/{directory}/change_access_type/{type}")
  void changeDirAccessType(@Param("type") String type, @Param("directory") long directory);

  @RequestLine("PUT /file/{file}/change_access_type/{type}")
  void changeFileAccessType(@Param("type") String type, @Param("file") long file);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("PUT /directories/change_access_type/{type}")
  void changeDirAccessType(@Param("type") String type, List<Long> dirs);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("PUT /files/change_access_type/{type}")
  void changeFileAccessType(@Param("type") String type, List<Long> files);

  @RequestLine("PUT /directory/{directory}/move_to/directory/{parent}")
  void moveDirectory(@Param("directory") long directory, @Param("parent") long parent);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("PUT /directories/move_to/directory/{parent}")
  void moveDirectories(List<Long> directories, @Param("parent") long parent);

  @RequestLine("PUT /file/{file}/move_to/directory/{parent}")
  void moveFile(@Param("file") long file, @Param("parent") long parent);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("PUT /files/move_to/directory/{parent}")
  void moveFiles(List<Long> files, @Param("parent") long parent);

  @RequestLine("DELETE /directory/{directory}")
  void deleteDir(@Param("directory")long directory);


  @RequestLine("DELETE /file/{file}")
  void deleteFile(@Param("file")long file);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("DELETE /files")
  void deleteFiles(List<Long> files);

  @Headers(value = {"Content-Type: application/json"})
  @RequestLine("DELETE /directories")
  void deleteDirs(List<Long> directories);

  @Headers("Content-Type: multipart/form-data")
  @RequestLine("POST /backup")
  Date doBackup(@Param("file") FormData file);


  @RequestLine("GET /backups")
  List<Date> getBackupList();

  //private val backupDateFormat: SimpleDateFormat = SimpleDateFormat("yyy_mm_dd_H_m")

  @RequestLine("GET /backup/{date}")
  byte[] getBackup(@Param("date") String date);

  @RequestLine("POST /directories?name={name}")
  DirectoryData createInRootDirectory(@Param("name") String name);

  @RequestLine("POST /directories/{parent}?name={name}")
  DirectoryData createDirectory(@Param("name") String name, @Param("parent") long parent);

  @RequestLine("PUT /directories/{dir}?name={name}")
  DirectoryData changeDirectoryName(@Param("name") String name,  @Param("dir") long parent);
}
