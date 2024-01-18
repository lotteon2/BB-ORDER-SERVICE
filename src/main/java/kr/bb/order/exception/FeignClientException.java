package kr.bb.order.exception;

public class FeignClientException extends RuntimeException {

  public FeignClientException(String message) {
    super(message);
  }
}
