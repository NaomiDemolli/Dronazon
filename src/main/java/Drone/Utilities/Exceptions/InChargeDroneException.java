package Drone.Utilities.Exceptions;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;

import javax.annotation.Nullable;

public class InChargeDroneException  extends StatusException {

    public InChargeDroneException(Status status) {
        super(status);
    }

    public InChargeDroneException(Status status, @Nullable Metadata trailers) {
        super(status, trailers);
    }

    public String getMessage() {
        return "InCharge";
    }
}
