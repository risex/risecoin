package rise;

public abstract class RiseException extends Exception {

    protected RiseException() {
        super();
    }

    protected RiseException(String message) {
        super(message);
    }

    protected RiseException(String message, Throwable cause) {
        super(message, cause);
    }

    protected RiseException(Throwable cause) {
        super(cause);
    }

    public static abstract class ValidationException extends RiseException {

        private ValidationException(String message) {
            super(message);
        }

        private ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class NotCurrentlyValidException extends ValidationException {

        public NotCurrentlyValidException(String message) {
            super(message);
        }

        public NotCurrentlyValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class NotYetEnabledException extends NotCurrentlyValidException {

        public NotYetEnabledException(String message) {
            super(message);
        }

        public NotYetEnabledException(String message, Throwable throwable) {
            super(message, throwable);
        }

    }

    public static final class NotValidException extends ValidationException {

        public NotValidException(String message) {
            super(message);
        }

        public NotValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class StopException extends RuntimeException {

        public StopException(String message) {
            super(message);
        }

        public StopException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
