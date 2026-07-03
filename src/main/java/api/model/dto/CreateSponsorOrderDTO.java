package api.model.dto;

public class CreateSponsorOrderDTO {
    private Integer amountYuan;
    private String note;

    public Integer getAmountYuan() {
        return amountYuan;
    }

    public void setAmountYuan(Integer amountYuan) {
        this.amountYuan = amountYuan;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
