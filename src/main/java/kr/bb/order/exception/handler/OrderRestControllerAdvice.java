package kr.bb.order.exception.handler;

import bloomingblooms.errors.DomainException;
import bloomingblooms.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderRestControllerAdvice {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<CommonResponse> domainException(DomainException e) {

    return ResponseEntity.ok()
        .body(CommonResponse.builder().errorCode(e.getMessage()).message(e.getMessage()).build());
  }
}
