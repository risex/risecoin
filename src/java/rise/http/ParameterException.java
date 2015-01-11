package rise.http;

import rise.RiseException;
import org.json.simple.JSONStreamAware;

final class ParameterException extends RiseException {

    private final JSONStreamAware errorResponse;

    ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    JSONStreamAware getErrorResponse() {
        return errorResponse;
    }

}
