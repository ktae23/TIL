AuthenticationUtils

```java
package com.openeg.openegscts.utils;

import com.openeg.openegscts.student.dto.UsersDto;
import com.openeg.openegscts.student.service.IUsersService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class AuthenticationUtils {


    @Autowired
    public AuthenticationUtils(IUsersService userService, Environment env) {
        this.userService = userService;
        this.env = env;
    }


    // "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@$!%*#?&])[A-Za-z[0-9]$@$!%*#?&]{8,15}$"
    // 테스트 계정들의 암호에 특수문자가 없어서 우선 특수문자 체크 부분은 빼놓음
    String pwPattern = "^(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z[0-9]$@$!%*#?&]{6,20}$";
    String pwPattern2 = "(.)\\1\\1\\1";

    private IUsersService userService;
    private Environment env;
    private long issueTime = System.currentTimeMillis();
    private final long access_expiration = issueTime + Long.parseLong(env.getProperty("token.access_expiration_time"));
    private final long refresh_expiration = issueTime + Long.parseLong(env.getProperty("token.refresh_expiration_time"));

    /**
     * 비밀번호 정책 체크
     * @param pw
     * @return boolean
     */
    public boolean checkPw(String pw){
        boolean result = true;
        String message = "";
        Matcher matcher = Pattern.compile(pwPattern).matcher(pw);
        Matcher matcher2 = Pattern.compile(pwPattern2).matcher(pw);
        if(!matcher.matches()){
            result = false;
            message = "Not 6-20 characters. ";
            log.debug("/login : !matcher.matches() " + message + " : "  + pw);
        } else {
            message = "ok 6-20";
            log.debug("/login ok : matcher.matches() " + message + " : "  + pw);
        }

        if(matcher2.find()){
            result = false;
            message = "Password contains repeating characters";
            log.debug("/login : matcher2.find() " + message + " : " + pw);
        } else {
            message = "ok - repeating";
            log.debug("/login ok : !matcher2.find() " + message + " : "  + pw);
        }
        return result;
    }

    /**
     *
     * @param request
     * @return
     */
    public String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        log.info(">>>> X-FORWARDED-FOR : " + ip);

        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
            log.info(">>>> Proxy-Client-IP : " + ip);
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP"); // 웹로직
            log.info(">>>> WL-Proxy-Client-IP : " + ip);
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
            log.info(">>>> HTTP_CLIENT_IP : " + ip);
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            log.info(">>>> HTTP_X_FORWARDED_FOR : " + ip);
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        log.info(">>>> Result : IP Address : " + ip);

        return ip;
    }


    public String makeAccessToken(UsersDto usersDto){
        return Jwts.builder()
                .setSubject("accessToken")
                .claim("userId", usersDto.getUserId())
                .claim("userEmail",usersDto.getEmail())
                .claim("freeYn",usersDto.getFreeYn())
                .claim("userName",usersDto.getName())
                .claim("userType",usersDto.getType())
                .claim("userPhone",usersDto.getPhone())
                .claim("status",usersDto.getStatus())
                .claim("allowLanguages",usersDto.getAllowLanguages())
                .setExpiration(Date.from(Instant.ofEpochMilli(access_expiration)))
                .setIssuedAt(Date.from(Instant.ofEpochMilli(issueTime)))
                .signWith(SignatureAlgorithm.HS512, env.getProperty("token.secret")).compact();
    }

    public String makeRefreshToken(UsersDto usersDto){
        return  Jwts.builder()
                .setSubject("refreshToken")
                .claim("userId", usersDto.getUserId())
                .setExpiration(Date.from(Instant.ofEpochMilli(refresh_expiration)))
                .setIssuedAt(Date.from(Instant.ofEpochMilli(issueTime)))
                .signWith(SignatureAlgorithm.HS512, env.getProperty("token.secret")).compact();
    }
}

```



authenticationFilter

```java
package com.openeg.openegscts.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openeg.openegscts.student.model.LoginRequestModel;
import com.openeg.openegscts.student.service.IUsersService;
import com.openeg.openegscts.student.dto.UsersDto;
import com.openeg.openegscts.utils.AuthenticationUtils;
import com.openeg.openegscts.utils.Const;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@CrossOrigin(origins = "*")
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private IUsersService userService;
    private Environment env;
    private AuthenticationUtils utils;


    @Autowired
    public AuthenticationFilter(AuthenticationManager authenticationManager, Environment env, IUsersService userService,  AuthenticationUtils utils) {
        this.userService = userService;
        this.env = env;
        this.utils = utils;
        super.setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.warn("================================================attemptAuthentication");


        try {

            LoginRequestModel creds = new ObjectMapper()
                    .readValue(request.getInputStream(), LoginRequestModel.class);


            // 	패스워드 정책 체크
        if (!utils.checkPw(creds.getPassword())) {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.sendError(HttpStatus.NOT_ACCEPTABLE.value(),"fail");
            response.addHeader("message", String.valueOf(Const.LOGIN_FAIL_PASSWORD_POLICY_VIOLATION));
        }

        // 	인증정보 조회
        UsersDto userDto;
        if (creds.getUserId() != null && !creds.getUserId().isEmpty()) {
            userDto = userService.confirmUser2(creds.getUserId(), creds.getPassword());
        } else {
            userDto = userService.confirmUser(creds.getEmail(), creds.getPassword());
        }

        //	일치하는 정보가 없는 경우
        if (userDto == null) {
            response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
            response.sendError(HttpStatus.NOT_ACCEPTABLE.value(),"fail");
            response.addHeader("message", String.valueOf(Const.LOGIN_FAIL_NO_MATCHING_ACCOUNT));
        }


            return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                    creds.getEmail(),
                    creds.getPassword(),
                    new ArrayList<>()
                    )
                );
            } catch (IOException ex) {
            log.warn("attemptAuthentication" + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        String email = ((User)authResult.getPrincipal()).getUsername();
        UsersDto usersDto = userService.getUserDetailsByEmail(email);

        String accessToken = utils.makeAccessToken(usersDto);
        String refreshToken = utils.makeRefreshToken(usersDto);

        log.warn("================================================successfulAuthentication userId" + usersDto.getUserId());
        //	다중 로그인 확인
        boolean isMultipleLogin = userService.checkMultipleLogin(usersDto.getUserId(), utils.getClientIP(request), utils.getIssueTime(), accessToken);
        if (isMultipleLogin) {
            response.addHeader("message", String.valueOf(Const.LOGIN_SUCCESS_MULTIPLE));
        } else {
            response.addHeader("message", String.valueOf(Const.LOGIN_SUCCESS));
        }
        response.addHeader("AccessToken", accessToken);
        response.addHeader("refreshToken", refreshToken);
        //TODO : userDto 내리기

    }




}
```

```
@Autowired
private Environment env;
private IUsersService userService;
private AuthenticationUtils utils;
private BCryptPasswordEncoder bCryptPasswordEncoder;

@Autowired
public WebSecurity(Environment env, IUsersService userService, AuthenticationUtils utils, BCryptPasswordEncoder bCryptPasswordEncoder) {
    this.env = env;
    this.userService = userService;
    this.utils = utils;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
}
```

```


.addFilterAt(new AuthenticationFilter(authenticationManager(), env, userService, utils), UsernamePasswordAuthenticationFilter.class)
.addFilterAt(new AuthorizationFilter(authenticationManager(), env, userService), BasicAuthenticationFilter.class)
```



 요청파라미터 래퍼

```java
package com.openeg.openegscts.security;

import com.openeg.openegscts.student.service.IUsersService;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
public class RequestAuthorizationFilter extends BasicAuthenticationFilter {

   Environment env;

   public RequestAuthorizationFilter(AuthenticationManager authenticationManager, Environment env) {
      super(authenticationManager);
      this.env = env;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
      System.out.println("요청 URI : " + request.getRequestURI());

      ReadableRequestWrapper wrapper = new ReadableRequestWrapper(request);
      String authorizationHeader = request.getHeader(env.getProperty("authorization.token.header.access-name"));

      String token = authorizationHeader.replace(env.getProperty("authorization.token.header.prefix"), "").replaceAll(" ", "");
      Claims claims = Jwts.parser().setSigningKey(env.getProperty("token.secret")).parseClaimsJws(token.trim()).getBody();

      if (wrapper.getParameter("userId") != null) {
         System.out.println(wrapper.getParameter("userId"));
      }

      Map<String, String[]> params = wrapper.getParameterMap();
      for(String[] p : params.values()){
         for(String i : p){
            System.out.println("파라미터 : " + i);
         }
      }

      String claimsUserId = (String) claims.get("userId");
      int claimsType = (int) claims.get("userType");
      int claimsStatus= (int) claims.get("status");
      String claimsFreeYn = (String) claims.get("freeYn");
      String claimsAllowedLanguages = (String) claims.get("allowLanguages");

      chain.doFilter(wrapper, response);
   }


   private class ReadableRequestWrapper extends HttpServletRequestWrapper { // 상속
      private final Charset encoding;
      private byte[] rawData;
      private Map<String, String[]> params = new HashMap<>();

      public ReadableRequestWrapper(HttpServletRequest request) {
         super(request);
         this.params.putAll(request.getParameterMap()); // 원래의 파라미터를 저장

         String charEncoding = request.getCharacterEncoding(); // 인코딩 설정
         this.encoding = StringUtils.isBlank(charEncoding) ? StandardCharsets.UTF_8 : Charset.forName(charEncoding);

         try {
            InputStream is = request.getInputStream();
            this.rawData = IOUtils.toByteArray(is); // InputStream 을 별도로 저장한 다음 getReader() 에서 새 스트림으로 생성

            // body 파싱
            String collect = this.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (StringUtils.isEmpty(collect)) { // body 가 없을경우 제외
               return;
            }
            if (request.getContentType() != null && request.getContentType().contains(
                  ContentType.MULTIPART_FORM_DATA.getMimeType())) { // 파일 업로드시 제외
               return;
            }
            JSONParser jsonParser = new JSONParser();
            Object parse = jsonParser.parse(collect);
            if (parse instanceof JSONArray) {
               JSONArray jsonArray = (JSONArray) jsonParser.parse(collect);
               setParameter("requestBody", jsonArray.toJSONString());
            } else {
               JSONObject jsonObject = (JSONObject) jsonParser.parse(collect);
               Iterator iterator = jsonObject.keySet().iterator();
               while (iterator.hasNext()) {
                  String key = (String) iterator.next();
                  setParameter(key, jsonObject.get(key).toString().replace("\"", "\\\""));
               }
            }
         } catch (Exception e) {
            log.error("ReadableRequestWrapper init error", e);
         }
      }

      @Override
      public String getParameter(String name) {
         String[] paramArray = getParameterValues(name);
         if (paramArray != null && paramArray.length > 0) {
            return paramArray[0];
         } else {
            return null;
         }
      }

      @Override
      public Map<String, String[]> getParameterMap() {
         return Collections.unmodifiableMap(params);
      }

      @Override
      public Enumeration<String> getParameterNames() {
         return Collections.enumeration(params.keySet());
      }

      @Override
      public String[] getParameterValues(String name) {
         String[] result = null;
         String[] dummyParamValue = params.get(name);

         if (dummyParamValue != null) {
            result = new String[dummyParamValue.length];
            System.arraycopy(dummyParamValue, 0, result, 0, dummyParamValue.length);
         }
         return result;
      }

      public void setParameter(String name, String value) {
         String[] param = {value};
         setParameter(name, param);
      }

      public void setParameter(String name, String[] values) {
         params.put(name, values);
      }

      @Override
      public ServletInputStream getInputStream() {
         final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.rawData);

         return new ServletInputStream() {
            @Override
            public boolean isFinished() {
               return false;
            }

            @Override
            public boolean isReady() {
               return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
               // Do nothing
            }

            public int read() {
               return byteArrayInputStream.read();
            }
         };
      }

      @Override
      public BufferedReader getReader() {
         return new BufferedReader(new InputStreamReader(this.getInputStream(), this.encoding));
      }
   }

}
```