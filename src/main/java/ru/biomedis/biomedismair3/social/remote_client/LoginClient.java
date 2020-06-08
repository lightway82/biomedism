package ru.biomedis.biomedismair3.social.remote_client;


import feign.Headers;
import feign.RequestLine;
import ru.biomedis.biomedismair3.social.remote_client.dto.Credentials;
import ru.biomedis.biomedismair3.social.remote_client.dto.Token;

//@Headers("Accept: application/json")
public interface LoginClient {

  @RequestLine("POST /request")
  @Headers("Content-Type: application/json")
  Token getToken(Credentials credentials);

  @RequestLine("POST /refresh")
  @Headers("Content-Type: application/json")
  Token refreshToken(String refresh);

}