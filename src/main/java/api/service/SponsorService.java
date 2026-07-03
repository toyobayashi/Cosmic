package api.service;

import api.model.dto.CreateSponsorOrderDTO;
import api.model.dto.SponsorAwardDTO;
import api.model.dto.SponsorOrderDTO;

import java.util.List;

public class SponsorService {
    public static final int NX_PER_YUAN = 100;
    public static final int MAX_AMOUNT_YUAN = 200;
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CLOSED = "CLOSED";

    private final SponsorRepository sponsorRepository;
    private final SponsorCashRefresher sponsorCashRefresher;

    public SponsorService() {
        this(new JdbiSponsorRepository(), new OnlineSponsorCashRefresher());
    }

    public SponsorService(SponsorRepository sponsorRepository) {
        this(sponsorRepository, new OnlineSponsorCashRefresher());
    }

    public SponsorService(SponsorRepository sponsorRepository, SponsorCashRefresher sponsorCashRefresher) {
        this.sponsorRepository = sponsorRepository;
        this.sponsorCashRefresher = sponsorCashRefresher;
    }

    public SponsorOrderDTO createOrder(int accountId, CreateSponsorOrderDTO request) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (request == null || request.getAmountYuan() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        int amountYuan = request.getAmountYuan();
        if (amountYuan < 1) {
            throw new IllegalArgumentException("Amount must be at least 1 CNY");
        }
        if (amountYuan > MAX_AMOUNT_YUAN) {
            throw new IllegalArgumentException("Amount cannot exceed 200 CNY");
        }

        int amountCents = amountYuan * 100;
        int expectedNx = amountYuan * NX_PER_YUAN;
        return sponsorRepository.createOrder(accountId, amountCents, expectedNx, trimToNull(request.getNote()));
    }

    public List<SponsorOrderDTO> listMyPendingOrders(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account ID is required");
        }
        return sponsorRepository.listOrdersByAccountId(accountId, STATUS_PENDING);
    }

    public List<SponsorOrderDTO> listMyOrders(int accountId) {
        if (accountId <= 0) {
            throw new IllegalArgumentException("Account ID is required");
        }
        return sponsorRepository.listOrdersByAccountId(accountId, null);
    }

    public List<SponsorOrderDTO> listOrders(String account, String status) {
        String normalizedAccount = trimToNull(account);
        return sponsorRepository.listOrders(normalizedAccount, normalizeStatus(status, normalizedAccount == null));
    }

    public SponsorOrderDTO awardOrder(String orderId, int awardedBy, SponsorAwardDTO request) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (awardedBy <= 0) {
            throw new IllegalArgumentException("Awarding account ID is required");
        }
        if (request == null || request.getAwardedNx() == null || request.getAwardedNx() <= 0) {
            throw new IllegalArgumentException("Awarded NX must be greater than 0");
        }
        SponsorOrderDTO order = sponsorRepository.awardPendingOrder(orderId, request.getAwardedNx(), awardedBy);
        if (order.getInternalAccountId() != null && order.getAwardedNx() != null) {
            sponsorCashRefresher.increaseNxCredit(order.getInternalAccountId(), order.getAwardedNx());
        }
        return order;
    }

    public SponsorOrderDTO closeOrder(String orderId, int closedBy) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (closedBy <= 0) {
            throw new IllegalArgumentException("Closing account ID is required");
        }
        return sponsorRepository.closePendingOrder(orderId, closedBy);
    }

    private String normalizeStatus(String status, boolean defaultPending) {
        if (status == null || status.isBlank()) {
            return defaultPending ? STATUS_PENDING : null;
        }
        return status.trim().toUpperCase();
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
