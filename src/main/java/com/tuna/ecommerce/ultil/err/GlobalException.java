package com.tuna.ecommerce.ultil.err;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tuna.ecommerce.domain.response.RestResponse;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleAllException(Exception ex){
        RestResponse<Object> res=new RestResponse<Object>();
        res.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setMessage(ex.getMessage());
        res.setError("Internal Exception");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @ExceptionHandler(value ={
        UsernameNotFoundException.class,
        BadCredentialsException.class})
    public ResponseEntity<RestResponse<Object>> IdInvalidException(Exception ex) {
       RestResponse<Object> res=new RestResponse<Object>();
       res.setStatusCode(HttpStatus.BAD_REQUEST.value());
       res.setError("Exception");
       res.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(res);
    }

        @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> validationErr(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        final List<FieldError> fieldErrors = result.getFieldErrors();

        RestResponse<Object> res=new RestResponse<Object>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError(ex.getBody().getDetail());
        List<String> errorMessage = fieldErrors.stream().map(f->f.getDefaultMessage()).collect(Collectors.toList());

        res.setMessage(errorMessage.size()>1 ? errorMessage : errorMessage.get(0));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(res);
    }

          @ExceptionHandler(value = { PermissionException.class })
    public ResponseEntity<RestResponse<Object>> handlePermissionException(Exception ex) {
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatusCode(HttpStatus.NOT_FOUND.value());
        res.setError("Forbidden");
        res.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

}
