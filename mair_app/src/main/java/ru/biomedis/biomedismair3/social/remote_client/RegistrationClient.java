package ru.biomedis.biomedismair3.social.remote_client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import ru.biomedis.biomedismair3.social.remote_client.dto.CityDto;
import ru.biomedis.biomedismair3.social.remote_client.dto.CountryDto;
import ru.biomedis.biomedismair3.social.remote_client.dto.RegistrationDto;


public interface RegistrationClient {

  /**
   * Осуществляет регистрацию
   * @param data данные клиента
   */
  @RequestLine("POST /")
  @Headers("Content-Type: application/json")
  void registration(RegistrationDto data);

  /**
   * Отправляет код подтверждения на почту
   * @param email
   */
  @RequestLine("GET /send_code?email={email}")
  void sendCode(@Param("email") String email);

  /**
   * Подтвердить почту по ранее высланому коду
   * @param email
   * @param code
   */
  @RequestLine("GET /confirm_email?email={email}&code={code}")
  void confirmEmail(@Param("email") String email, @Param("code") String code);

  /**
   * Сбросить пароль и отправить проверочный код на почту
   * @param email
   */
  @RequestLine("GET /send_reset_code?email={email}")
  void sendResetCode(@Param("email") String email);


  /**
   * Задать новый пароль по коду  сброса
   * @param email
   * @param code
   * @param password
   */
  @RequestLine("PUT /new_password?email={email}&password={password}&code={code}")
  void setNewPassword(@Param("email") String email, @Param("code") String code, @Param("password") String password);

  /**
   * Список стран
   */
  @RequestLine("GET /countries")
  List<CountryDto> countriesList();


  @RequestLine("GET /country/{country}/cities")
  List<CityDto> citiesList(@Param("country") long country);
}
