package api.model.dto;

public class ServerShutdownDTO {
    private String message;
    private int minutes;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
}
