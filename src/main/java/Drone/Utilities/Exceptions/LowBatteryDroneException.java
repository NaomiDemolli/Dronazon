package Drone.Utilities.Exceptions;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import javax.annotation.Nullable;

public class LowBatteryDroneException extends StatusException {

    public LowBatteryDroneException(Status status) {
        super(status);
    }

    public LowBatteryDroneException(Status status, @Nullable Metadata trailers) {
        super(status, trailers);
    }

    @Override
    public String getMessage() {
        return "LowBattery";
    }
}
