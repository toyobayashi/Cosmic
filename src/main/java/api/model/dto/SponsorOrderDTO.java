package api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SponsorOrderDTO {
    private String orderId;
    private String account;
    private Integer internalAccountId;
    private Integer amountCents;
    private Integer expectedNx;
    private Integer awardedNx;
    private String status;
    private String note;
    private String createdAt;
    private String updatedAt;
    private Integer updatedBy;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @JsonIgnore
    public Integer getInternalAccountId() {
        return internalAccountId;
    }

    public void setInternalAccountId(Integer internalAccountId) {
        this.internalAccountId = internalAccountId;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public Integer getExpectedNx() {
        return expectedNx;
    }

    public void setExpectedNx(Integer expectedNx) {
        this.expectedNx = expectedNx;
    }

    public Integer getAwardedNx() {
        return awardedNx;
    }

    public void setAwardedNx(Integer awardedNx) {
        this.awardedNx = awardedNx;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }
}
