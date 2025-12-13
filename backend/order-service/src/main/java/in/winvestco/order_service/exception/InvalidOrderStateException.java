package in.winvestco.order_service.exception;

import in.winvestco.common.enums.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrderStateException extends RuntimeException {
    
    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, String action) {
        super(String.format("Cannot %s order %s in state %s", action, orderId, currentStatus));
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
