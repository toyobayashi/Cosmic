package api.model;

public class ResultBody<T> {
    private int code;
    private String message;
    private T data;

    public ResultBody() {}

    public ResultBody(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultBody<T> success(T data) {
        return new ResultBody<>(200, "Success", data);
    }

    public static <T> ResultBody<T> success() {
        return new ResultBody<>(200, "Success", null);
    }

    public static <T> ResultBody<T> error(int code, String message) {
        return new ResultBody<>(code, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
