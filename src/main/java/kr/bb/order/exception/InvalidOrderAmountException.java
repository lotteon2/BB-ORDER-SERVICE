package kr.bb.order.exception;

import kr.bb.order.exception.common.ErrorCode;

public class InvalidOrderAmountException extends RuntimeException{
    private static final String message = ErrorCode.INVALID_ORDER_ACTUAL_AMOUNT.getMessage();

    public InvalidOrderAmountException() {super(message);}
}
