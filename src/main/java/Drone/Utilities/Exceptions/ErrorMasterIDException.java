package Drone.Utilities.Exceptions;

import io.grpc.Status;
import io.grpc.StatusException;

public class ErrorMasterIDException extends StatusException {


    public ErrorMasterIDException(Status status) {
        super(status);
    }

    public String getMessage() {
        return "ErrorMasterID";
    }
}
