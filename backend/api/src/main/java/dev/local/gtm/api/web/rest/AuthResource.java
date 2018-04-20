package dev.local.gtm.api.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.local.gtm.api.config.AppProperties;
import dev.local.gtm.api.domain.Auth;
import dev.local.gtm.api.service.AuthService;
import dev.local.gtm.api.web.exception.InvalidPasswordException;
import dev.local.gtm.api.web.rest.vm.KeyAndPasswordVM;
import dev.local.gtm.api.web.rest.vm.UserVM;
import io.swagger.annotations.ApiOperation;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户鉴权资源接口
 *
 * @author Peng Wang (wpcfan@gmail.com)
 */
@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;
    private final AppProperties appProperties;

    @ApiOperation(value = "用户登录鉴权接口",
            notes = "客户端在 RequestBody 中以 json 传入用户名、密码，如果成功以 json 形式返回该用户信息")
    @PostMapping(value = "/auth/login")
    public ResponseEntity<JWTToken> login(@RequestBody final Auth auth) {
        log.debug("REST 请求 -- 将对用户: {} 执行登录鉴权", auth);
        val jwt = authService.login(auth.getLogin(), auth.getPassword());
        val headers = new HttpHeaders();
        headers.add(appProperties.getSecurity().getAuthorization().getHeader(), "Bearer " + jwt);
        log.debug("JWT token {} 加入到 HTTP 头", jwt);
        return new ResponseEntity<>(new JWTToken(jwt), headers, HttpStatus.OK);
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody UserVM userVM) {
        log.debug("REST 请求 -- 注册用户: {} ", userVM);
        if (!checkPasswordLength(userVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        authService.registerUser(userVM, userVM.getPassword());
    }

    @PostMapping(value = "/auth/mobile")
    @ResponseStatus(value = HttpStatus.OK)
    public ResetKey verifyMobile(@RequestBody MobileVerification verification) {
        log.debug("REST 请求 -- 验证手机号 {} 和短信验证码 {}", verification.getMobile(), verification.getCode());
        val key = authService.verifyMobile(verification.getMobile(), verification.getCode());
        return new ResetKey(key);
    }

    @PostMapping(value = "/auth/reset")
    public void resetPassword(@RequestBody KeyAndPasswordVM keyAndPasswordVM) {
        log.debug("REST 请求 -- 重置密码 {}", keyAndPasswordVM);
        authService.resetPassword(keyAndPasswordVM.getResetKey(), keyAndPasswordVM.getMobile(), keyAndPasswordVM.getPassword());
    }

    @PostMapping("/auth/captchaVerification")
    public CaptchaResult verifyCaptcha(@RequestBody final CaptchaVerification verification) {
        log.debug("REST 请求 -- 验证 Captcha {}", verification);
        val result = authService.verifyCaptcha(verification.getCode(), verification.getToken());
        log.debug("Captcha 验证返回结果 {}", verification);
        return new CaptchaResult(result);
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
                password.length() >= UserVM.PASSWORD_MIN_LENGTH &&
                password.length() <= UserVM.PASSWORD_MAX_LENGTH;
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    private static class JWTToken {
        @JsonProperty("id_token")
        private String idToken;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptchaVerification {
        @JsonProperty("captcha_token")
        private String token;
        @JsonProperty("captcha_code")
        private String code;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MobileVerification {
        private String mobile;
        private String code;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ResetKey {
        @JsonProperty("reset_key")
        private String resetKey;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CaptchaResult {
        @JsonProperty("validate_token")
        private String validatedToken;
    }
}
