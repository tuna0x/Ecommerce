package com.tuna.ecommerce.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.auth.ReqLoginDTO;
import com.tuna.ecommerce.domain.request.auth.ReqRegisterDTO;
import com.tuna.ecommerce.domain.response.RestLoginDTO;
import com.tuna.ecommerce.domain.request.auth.ReqSocialLoginDTO;
import com.tuna.ecommerce.domain.request.auth.ReqChangePasswordDTO;
import com.tuna.ecommerce.domain.request.auth.ReqResetPasswordDTO;
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.service.OtpService;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;

@RequestMapping("/api/v1")
@RestController
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Value("${tuna.google.client-id}")
    private String googleClientId;

    @Value("${tuna.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtil securityUtil,
            UserService userService, PasswordEncoder passwordEncoder, OtpService otpService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @PostMapping("/auth/login")
    @APIMessage("User logged in successfully")
    public ResponseEntity<RestLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO,
            jakarta.servlet.http.HttpServletRequest request) throws IdInvalidException {
        String ip = request.getRemoteAddr();
        this.userService.updateLoginInfo(loginDTO.getUsername(), ip);

        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication;
        try {
            authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new IdInvalidException(
                    "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new IdInvalidException("Email hoặc mật khẩu không chính xác.");
        } catch (Exception e) {
            throw new IdInvalidException("Có lỗi xảy ra trong quá trình đăng nhập. Vui lòng thử lại sau.");
        }

        // set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RestLoginDTO res = new RestLoginDTO();
        User curUserDB = this.userService.findByUsername(loginDTO.getUsername());
        if (curUserDB != null) {
            RestLoginDTO.UserLogin userLogin = new RestLoginDTO.UserLogin();
            userLogin.setId(curUserDB.getId());
            userLogin.setEmail(curUserDB.getEmail());
            userLogin.setRole(curUserDB.getRole());
            if (curUserDB.getUserProfile() != null) {
                userLogin.setName(curUserDB.getUserProfile().getName());
                userLogin.setImage(curUserDB.getUserProfile().getImage());
                userLogin.setAge(curUserDB.getUserProfile().getAge());
                userLogin.setGender(curUserDB.getUserProfile().getGender());
            }
            userLogin.setVerified(curUserDB.getVerified());
            res.setUser(userLogin);
        }
        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);

        // create refresh token
        String refresh_token = this.securityUtil.refreshToken(loginDTO.getUsername(), res);

        // update user
        this.userService.updateUserToken(refresh_token, loginDTO.getUsername());

        // set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("/auth/social-login")
    @APIMessage("Google login successfully")
    public ResponseEntity<RestLoginDTO> googleLogin(@Valid @RequestBody ReqSocialLoginDTO loginDTO,
            jakarta.servlet.http.HttpServletRequest request) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())

                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(loginDTO.getIdToken());
        if (idToken == null) {
            throw new IdInvalidException("Invalid Id Token");
        }

        Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String ip = request.getRemoteAddr();
        this.userService.updateLoginInfo(email, ip);

        User user = this.userService.findByUsername(email);
        if (user == null) {
            // Create new user
            ReqRegisterDTO reg = new ReqRegisterDTO();
            reg.setEmail(email);
            reg.setName(name);
            reg.setPassword(this.passwordEncoder.encode("GOOGLE_PASS_" + Math.random()));
            user = this.userService.register(reg);

            // Update image from google if possible
            if (pictureUrl != null && user.getUserProfile() != null) {
                user.getUserProfile().setImage(pictureUrl);
                this.userService.handleUpdate(user);
            }
        }

        // Auto verify google user
        if (user.getVerified() == null || !user.getVerified()) {
            user.setVerified(true);
            this.userService.handleUpdate(user);
        }

        // Check if user is active
        if (user.getActive() != null && !user.getActive()) {
            throw new IdInvalidException(
                    "Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        }

        RestLoginDTO res = new RestLoginDTO();
        RestLoginDTO.UserLogin userLogin = new RestLoginDTO.UserLogin();
        userLogin.setId(user.getId());
        userLogin.setEmail(user.getEmail());
        userLogin.setRole(user.getRole());
        if (user.getUserProfile() != null) {
            userLogin.setName(user.getUserProfile().getName());
            userLogin.setImage(user.getUserProfile().getImage());
            userLogin.setAge(user.getUserProfile().getAge());
            userLogin.setGender(user.getUserProfile().getGender());
        }
        userLogin.setVerified(user.getVerified());
        res.setUser(userLogin);

        String access_token = this.securityUtil.createAccessToken(user.getEmail(), res);
        res.setAccessToken(access_token);

        String refresh_token = this.securityUtil.refreshToken(user.getEmail(), res);
        this.userService.updateUserToken(refresh_token, user.getEmail());

        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @GetMapping("/auth/account")
    @APIMessage("Get current user account successfully")
    public ResponseEntity<RestLoginDTO.UserGetAccount> getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        User curUser = this.userService.findByUsername(email);
        RestLoginDTO.UserLogin userLogin = new RestLoginDTO.UserLogin();
        RestLoginDTO.UserGetAccount userGetAccount = new RestLoginDTO.UserGetAccount();

        if (curUser != null) {
            userLogin.setId(curUser.getId());
            userLogin.setEmail(curUser.getEmail());
            if (curUser.getUserProfile() != null) {
                userLogin.setName(curUser.getUserProfile().getName());
                userLogin.setImage(curUser.getUserProfile().getImage());
                userLogin.setAge(curUser.getUserProfile().getAge());
                userLogin.setGender(curUser.getUserProfile().getGender());
            }
            userLogin.setVerified(curUser.getVerified());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok().body(userGetAccount);
    }

    @GetMapping("/auth/permissions")
    @APIMessage("Get current user permissions successfully")
    public ResponseEntity<List<com.tuna.ecommerce.domain.response.user.ResUserPermissionDTO>> getPermissions() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        return ResponseEntity.ok().body(this.userService.getPermissionsByEmail(email));
    }

    @GetMapping("/auth/refresh")
    @APIMessage("Refresh token successfully")
    public ResponseEntity<RestLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "") String refresh_token) throws IdInvalidException {
        if (refresh_token.equals("")) {
            throw new IdInvalidException("Session has expired, please login again...");
        }

        // Check valid
        Jwt decodedToken = this.securityUtil.checkValidToken(refresh_token);
        String email = decodedToken.getSubject();

        // check user by token + email
        User curUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (curUser == null) {
            throw new IdInvalidException("Invalid refresh token");
        }

        // issue new token/ set refresh token as cookies
        RestLoginDTO res = new RestLoginDTO();
        User curUserDB = this.userService.findByUsername(email);
        if (curUserDB != null) {
            RestLoginDTO.UserLogin userLogin = new RestLoginDTO.UserLogin();
            userLogin.setId(curUserDB.getId());
            userLogin.setEmail(curUserDB.getEmail());
            userLogin.setRole(curUserDB.getRole());
            if (curUserDB.getUserProfile() != null) {
                userLogin.setName(curUserDB.getUserProfile().getName());
                userLogin.setImage(curUserDB.getUserProfile().getImage());
                userLogin.setAge(curUserDB.getUserProfile().getAge());
                userLogin.setGender(curUserDB.getUserProfile().getGender());
            }
            userLogin.setVerified(curUserDB.getVerified());
            res.setUser(userLogin);
        }
        String access_token = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        // create refresh token
        String new_refresh_token = this.securityUtil.refreshToken(email, res);

        // update user
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("/auth/logout")
    @APIMessage("User logged out successfully")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null || email.isEmpty()) {
            throw new IdInvalidException("Access token không hợp lệ");
        }
        this.userService.updateUserToken(null, email);

        ResponseCookie deletResponseCookie = ResponseCookie
                .from("refresh_token", email)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, deletResponseCookie.toString())
                .body(null);
    }

    @PostMapping("/auth/register")
    @APIMessage("User registered successfully")
    public ResponseEntity<RestLoginDTO> register(@Valid @RequestBody ReqRegisterDTO registerDTO)
            throws IdInvalidException {
        boolean isEmailExits = this.userService.exitsByEmail(registerDTO.getEmail());
        if (isEmailExits) {
            throw new IdInvalidException("email is exists");
        }

        String hashPassWord = this.passwordEncoder.encode(registerDTO.getPassword());
        registerDTO.setPassword(hashPassWord);
        User newUser = this.userService.register(registerDTO);

        // --- AUTO LOGIN LOGIC ---
        RestLoginDTO res = new RestLoginDTO();
        RestLoginDTO.UserLogin userLogin = new RestLoginDTO.UserLogin();
        userLogin.setId(newUser.getId());
        userLogin.setEmail(newUser.getEmail());
        userLogin.setRole(newUser.getRole());

        if (newUser.getUserProfile() != null) {
            userLogin.setName(newUser.getUserProfile().getName());
            userLogin.setImage(newUser.getUserProfile().getImage());
            userLogin.setAge(newUser.getUserProfile().getAge());
            userLogin.setGender(newUser.getUserProfile().getGender());
        }
        userLogin.setVerified(newUser.getVerified());
        res.setUser(userLogin);

        // Create tokens
        String access_token = this.securityUtil.createAccessToken(newUser.getEmail(), res);
        res.setAccessToken(access_token);
        String refresh_token = this.securityUtil.refreshToken(newUser.getEmail(), res);

        // Update user token
        this.userService.updateUserToken(refresh_token, newUser.getEmail());

        // Set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @APIMessage("Kiểm tra email thành công")
    @GetMapping("/auth/check-email")
    public ResponseEntity<Boolean> checkEmail(@org.springframework.web.bind.annotation.RequestParam String email) {
        return ResponseEntity.ok(this.userService.exitsByEmail(email));
    }

    @PostMapping("/auth/otp/send")
    @APIMessage("Mã OTP đã được gửi đến email của bạn")
    public ResponseEntity<Void> sendOtp(@RequestBody java.util.Map<String, String> request) throws IdInvalidException {
        String email = request.get("email");
        boolean isEmailExits = this.userService.exitsByEmail(email);
        if (isEmailExits) {
            throw new IdInvalidException("Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }
        this.otpService.generateAndSendOtp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/otp/verify")
    @APIMessage("Xác thực OTP thành công")
    public ResponseEntity<Void> verifyOtp(@RequestBody java.util.Map<String, String> request)
            throws IdInvalidException {
        String email = request.get("email");
        String otp = request.get("otp");
        boolean isValid = this.otpService.verifyOtp(email, otp);
        if (!isValid) {
            throw new IdInvalidException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        // Update user verified status
        User user = this.userService.findByUsername(email);
        if (user != null) {
            user.setVerified(true);
            this.userService.handleUpdate(user);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/forgot-password")
    @APIMessage("Mã OTP đã được gửi đến email của bạn")
    public ResponseEntity<Void> forgotPassword(@RequestBody java.util.Map<String, String> request)
            throws IdInvalidException {
        String email = request.get("email");
        boolean isEmailExits = this.userService.exitsByEmail(email);
        if (!isEmailExits) {
            throw new IdInvalidException("Email này chưa được đăng ký trong hệ thống.");
        }
        this.otpService.generateAndSendOtp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/reset-password")
    @APIMessage("Đặt lại mật khẩu thành công")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ReqResetPasswordDTO req)
            throws IdInvalidException {
        boolean isValid = this.otpService.verifyOtp(req.getEmail(), req.getOtp());
        if (!isValid) {
            throw new IdInvalidException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        String hashPassWord = this.passwordEncoder.encode(req.getNewPassword());
        this.userService.updatePassword(req.getEmail(), hashPassWord);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/change-password")
    @APIMessage("Đổi mật khẩu thành công")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ReqChangePasswordDTO req)
            throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) {
            throw new IdInvalidException("Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn");
        }

        User user = this.userService.findByUsername(email);
        if (!this.passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IdInvalidException("Mật khẩu hiện tại không chính xác");
        }

        String hashPassWord = this.passwordEncoder.encode(req.getNewPassword());
        this.userService.updatePassword(email, hashPassWord);
        return ResponseEntity.ok().build();
    }
}
