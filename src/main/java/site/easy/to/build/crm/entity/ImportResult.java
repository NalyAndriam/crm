package site.easy.to.build.crm.entity;

public class ImportResult {
    private final boolean success;
    private final String message;
    private final String errorLine;

    public ImportResult(boolean success, String message, String errorLine) {
        this.success = success;
        this.message = message;
        this.errorLine = errorLine;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorLine() {
        return errorLine;
    }
}