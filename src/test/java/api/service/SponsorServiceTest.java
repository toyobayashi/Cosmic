package api.service;

import api.model.dto.CreateSponsorOrderDTO;
import api.model.dto.SponsorAwardDTO;
import api.model.dto.SponsorOrderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SponsorServiceTest {

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private SponsorCashRefresher sponsorCashRefresher;

    private SponsorService sponsorService;

    @BeforeEach
    void reset() {
        MockitoAnnotations.openMocks(this);
        sponsorService = new SponsorService(sponsorRepository, sponsorCashRefresher);
    }

    @Test
    void createOrderStoresPendingAmountAndExpectedNx() {
        CreateSponsorOrderDTO request = new CreateSponsorOrderDTO();
        request.setAmountYuan(20);
        request.setNote("wechat note");

        SponsorOrderDTO saved = new SponsorOrderDTO();
        saved.setOrderId("66b2f8e40000000000000001");
        saved.setAccount("toyobayashi");
        saved.setInternalAccountId(100);
        saved.setAmountCents(2000);
        saved.setExpectedNx(2000);
        saved.setStatus("PENDING");
        saved.setNote("wechat note");
        when(sponsorRepository.createOrder(100, 2000, 2000, "wechat note")).thenReturn(saved);

        SponsorOrderDTO result = sponsorService.createOrder(100, request);

        assertEquals("66b2f8e40000000000000001", result.getOrderId());
        assertEquals(2000, result.getAmountCents());
        assertEquals(2000, result.getExpectedNx());
        verify(sponsorRepository).createOrder(100, 2000, 2000, "wechat note");
    }

    @Test
    void createOrderRejectsAmountsAboveTwoHundredYuan() {
        CreateSponsorOrderDTO request = new CreateSponsorOrderDTO();
        request.setAmountYuan(201);

        assertThrows(IllegalArgumentException.class, () -> sponsorService.createOrder(100, request));
    }

    @Test
    void listPendingOrdersUsesAccountAndPendingStatus() {
        SponsorOrderDTO order = new SponsorOrderDTO();
        order.setOrderId("66b2f8e40000000000000009");
        when(sponsorRepository.listOrdersByAccountId(100, "PENDING")).thenReturn(List.of(order));

        List<SponsorOrderDTO> result = sponsorService.listMyPendingOrders(100);

        assertEquals(1, result.size());
        assertEquals("66b2f8e40000000000000009", result.get(0).getOrderId());
        verify(sponsorRepository).listOrdersByAccountId(100, "PENDING");
    }

    @Test
    void listMyOrdersUsesAccountWithoutStatusFilter() {
        SponsorOrderDTO order = new SponsorOrderDTO();
        order.setOrderId("66b2f8e40000000000000011");
        when(sponsorRepository.listOrdersByAccountId(100, null)).thenReturn(List.of(order));

        List<SponsorOrderDTO> result = sponsorService.listMyOrders(100);

        assertEquals(1, result.size());
        assertEquals("66b2f8e40000000000000011", result.get(0).getOrderId());
        verify(sponsorRepository).listOrdersByAccountId(100, null);
    }

    @Test
    void listOrdersWithAccountNameAndBlankStatusReturnsAllStatusesForHistory() {
        SponsorOrderDTO order = new SponsorOrderDTO();
        order.setOrderId("66b2f8e40000000000000010");
        order.setAccount("toyobayashi");
        when(sponsorRepository.listOrders("toyobayashi", null)).thenReturn(List.of(order));

        List<SponsorOrderDTO> result = sponsorService.listOrders("toyobayashi", "");

        assertEquals(1, result.size());
        assertEquals("66b2f8e40000000000000010", result.get(0).getOrderId());
        assertEquals("toyobayashi", result.get(0).getAccount());
        verify(sponsorRepository).listOrders("toyobayashi", null);
    }

    @Test
    void awardOrderUsesOverrideAmountWhenProvided() {
        SponsorAwardDTO award = new SponsorAwardDTO();
        award.setAwardedNx(1234);

        SponsorOrderDTO awarded = new SponsorOrderDTO();
        awarded.setOrderId("66b2f8e40000000000000088");
        awarded.setAccount("toyobayashi");
        awarded.setInternalAccountId(100);
        awarded.setAwardedNx(1234);
        awarded.setStatus("AWARDED");
        when(sponsorRepository.awardPendingOrder("66b2f8e40000000000000088", 1234, 200)).thenReturn(awarded);

        sponsorService.awardOrder("66b2f8e40000000000000088", 200, award);

        verify(sponsorRepository).awardPendingOrder("66b2f8e40000000000000088", 1234, 200);
        verify(sponsorCashRefresher).increaseNxCredit(100, 1234);
    }

    @Test
    void awardOrderRejectsInvalidOverrideAmount() {
        SponsorAwardDTO award = new SponsorAwardDTO();
        award.setAwardedNx(0);

        assertThrows(IllegalArgumentException.class, () -> sponsorService.awardOrder("66b2f8e40000000000000088", 200, award));
    }

    @Test
    void closeOrderClosesPendingOrder() {
        SponsorOrderDTO closed = new SponsorOrderDTO();
        closed.setOrderId("66b2f8e40000000000000088");
        closed.setStatus("CLOSED");
        when(sponsorRepository.closePendingOrder("66b2f8e40000000000000088", 200)).thenReturn(closed);

        SponsorOrderDTO result = sponsorService.closeOrder("66b2f8e40000000000000088", 200);

        assertEquals("CLOSED", result.getStatus());
        verify(sponsorRepository).closePendingOrder("66b2f8e40000000000000088", 200);
    }

    @Test
    void closeOrderRejectsInvalidOrderId() {
        assertThrows(IllegalArgumentException.class, () -> sponsorService.closeOrder("", 200));
    }
}
