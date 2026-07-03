package api.service;

import api.model.dto.SponsorOrderDTO;

import java.util.List;

public interface SponsorRepository {
    SponsorOrderDTO createOrder(int accountId, int amountCents, int expectedNx, String note);

    List<SponsorOrderDTO> listOrders(String account, String status);

    List<SponsorOrderDTO> listOrdersByAccountId(int accountId, String status);

    SponsorOrderDTO awardPendingOrder(String orderId, int awardedNx, int awardedBy);

    SponsorOrderDTO closePendingOrder(String orderId, int closedBy);
}
