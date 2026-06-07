package api.model;

public class SubmitBody<T> {
    private T data;

    public SubmitBody() {}

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
