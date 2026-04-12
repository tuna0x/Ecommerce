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
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.service.OtpService;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.validation.Valid;

@RequestMapping("/api/v1")
@RestController
public class AuthController {
        private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

        @Value("${tuna.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder
     , SecurityUtil securityUtil,UserService userService,PasswordEncoder passwordEncoder, OtpService otpService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.passwordEncoder=passwordEncoder;
        this.otpService = otpService;
    }


    @PostMapping("/auth/login")
    @APIMessage("User logged in successfully")
    public ResponseEntity<RestLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO){
        // Nạp input gồm username/password vào Security
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
    // xác thực người dùng => cần viết hàm loadUserByUsername
    Authentication authentication=authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    // set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

    RestLoginDTO res=new RestLoginDTO();
        User curUserDB=this.userService.findByUsername(loginDTO.getUsername());
        if (curUserDB!=null) {
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
            res.setUser(userLogin);

        }
           String access_token= this.securityUtil.createAccessToken(authentication.getName(),res);
        res.setAccessToken(access_token);


        //create refresh token
        String refresh_token=this.securityUtil.refreshToken(loginDTO.getUsername(),res);

        //update user
        this.userService.updateUserToken(refresh_token,loginDTO.getUsername());

        //set cookie
        ResponseCookie resCookies=ResponseCookie.
        from("refresh_token", refresh_token).
        httpOnly(true).
        secure(true).
        path("/").
        maxAge(refreshTokenExpiration).
        build();
    return ResponseEntity.ok()
    .header(org.springframework.http.HttpHeaders.SET_COOKIE,resCookies.toString())
    .body(res);
    }



    @GetMapping("/auth/account")
    @APIMessage("Get current user account successfully")
    public ResponseEntity<RestLoginDTO.UserGetAccount> getAccount() {
        String email=SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
            User curUser=this.userService.findByUsername(email);
         RestLoginDTO.UserLogin userLogin=new RestLoginDTO.UserLogin();
         RestLoginDTO.UserGetAccount userGetAccount=new RestLoginDTO.UserGetAccount();

            if (curUser != null) {
                userLogin.setId(curUser.getId());
                userLogin.setEmail(curUser.getEmail());
                if (curUser.getUserProfile() != null) {
                    userLogin.setName(curUser.getUserProfile().getName());
                    userLogin.setImage(curUser.getUserProfile().getImage());
                    userLogin.setAge(curUser.getUserProfile().getAge());
                    userLogin.setGender(curUser.getUserProfile().getGender());
                }
                userGetAccount.setUser(userLogin);
            }
        return ResponseEntity.ok().body(userGetAccount);
    }


        @GetMapping("/auth/refresh")
        @APIMessage("Refresh token successfully")
    public ResponseEntity<RestLoginDTO> getRefreshToken(@CookieValue(name="refresh_token",defaultValue = "") String refresh_token) throws IdInvalidException{
        if (refresh_token.equals("")) {
            throw new IdInvalidException("Session has expired, please login again...");
        }

        //Check valid
        Jwt decodedToken=this.securityUtil.checkValidToken(refresh_token);
        String email=decodedToken.getSubject();

        //check user by token + email
        User curUser=this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (curUser==null) {
            throw new IdInvalidException("Invalid refresh token");
        }

        // issue new token/ set refresh token as cookies
        RestLoginDTO res=new RestLoginDTO();
        User curUserDB=this.userService.findByUsername(email);
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
            res.setUser(userLogin);
        }
           String access_token= this.securityUtil.createAccessToken(email,res);
        res.setAccessToken(access_token);


        //create refresh token
        String new_refresh_token=this.securityUtil.refreshToken(email,res);

        //update user
        this.userService.updateUserToken(new_refresh_token,email);

        //set cookie
        ResponseCookie resCookies=ResponseCookie.
        from("refresh_token", new_refresh_token).
        httpOnly(true).
        secure(true).
        path("/").
        maxAge(refreshTokenExpiration).
        build();
    return ResponseEntity.ok()
    .header(org.springframework.http.HttpHeaders.SET_COOKIE,resCookies.toString())
    .body(res);
    }

    @PostMapping("/auth/logout")
    @APIMessage("User logged out successfully")
    public ResponseEntity<Void> logout() throws IdInvalidException{
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null || email.isEmpty()) {
            throw new IdInvalidException("Access token không hợp lệ");
        }
        this.userService.updateUserToken(null, email);

        ResponseCookie deletResponseCookie =ResponseCookie
        .from("refresh_token", email)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .build();

        return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.SET_COOKIE,deletResponseCookie.toString())
        .body(null);
    }

    @PostMapping("/auth/register")
    @APIMessage("User registered successfully")
    public ResponseEntity<ResCreateUser> register(@Valid @RequestBody ReqRegisterDTO user) throws IdInvalidException{
        boolean isEmailExits=this.userService.exitsByEmail(user.getEmail());
        if (isEmailExits) {
            throw new IdInvalidException("email is exists");
        }

        String hashPassWord =this.passwordEncoder.encode(user.getPassword());
        user.setPassword(hashPassWord);
        User cur=this.userService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUser(cur));
    }

    @PostMapping("/auth/otp/send")
    @APIMessage("Mã OTP đã được gửi đến email của bạn")
    public ResponseEntity<Void> sendOtp(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        this.otpService.generateAndSendOtp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/otp/verify")
    @APIMessage("Xác thực OTP thành công")
    public ResponseEntity<Void> verifyOtp(@RequestBody java.util.Map<String, String> request) throws IdInvalidException {
        String email = request.get("email");
        String otp = request.get("otp");
        boolean isValid = this.otpService.verifyOtp(email, otp);
        if (!isValid) {
            throw new IdInvalidException("Mã OTP không chính xác hoặc đã hết hạn");
        }
        return ResponseEntity.ok().build();
    }
}
