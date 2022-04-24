package Drone.Utilities.Exceptions;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;

import javax.annotation.Nullable;

public class NotAvailableDroneException extends StatusException {

    public NotAvailableDroneException(Status status) {
        super(status);
    }

    public NotAvailableDroneException(Status status, @Nullable Metadata trailers) {
        super(status, trailers);
    }

    @Override
    public String getMessage() {
        return "NotAvailable";
    }
}
