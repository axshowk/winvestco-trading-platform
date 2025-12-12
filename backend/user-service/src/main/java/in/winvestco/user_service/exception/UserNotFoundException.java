package in.winvestco.user_service.exception;

import in.winvestco.common.exception.BaseException;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND",
              String.format("User not found with id: %d", id),
              String.format("User not found with id: %d", id));
    }

    public UserNotFoundException(String currentUserLogin) {
        super("USER_NOT_FOUND",
              String.format("User not found: %s", currentUserLogin),
              String.format("User not found: %s", currentUserLogin));
    }
}
