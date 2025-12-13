package in.winvestco.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
