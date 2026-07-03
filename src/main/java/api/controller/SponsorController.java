package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.model.dto.CreateSponsorOrderDTO;
import api.model.dto.SponsorAwardDTO;
import api.model.dto.SponsorOrderDTO;
import api.service.SponsorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsor")
public class SponsorController {

    @Autowired
    private HttpServletRequest request;

    private static final SponsorService sponsorService = new SponsorService();

    @Tag(name = "/sponsor/" + ApiConstant.LATEST)
    @Operation(summary = "List sponsor orders")
    @GetMapping("/" + ApiConstant.LATEST + "/orders")
    public ResultBody<List<SponsorOrderDTO>> listOrders(
            @RequestParam(name = "account", required = false) String account,
            @RequestParam(name = "status", required = false) String status) {
        try {
            return ResultBody.success(sponsorService.listOrders(account, status));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to list sponsor orders: " + e.getMessage());
        }
    }

    @Tag(name = "/sponsor/" + ApiConstant.LATEST)
    @Operation(summary = "List current account pending sponsor orders")
    @GetMapping("/" + ApiConstant.LATEST + "/orders/my-pending")
    public ResultBody<List<SponsorOrderDTO>> myPendingOrders() {
        try {
            int accountId = (int) request.getAttribute("accountId");
            return ResultBody.success(sponsorService.listMyPendingOrders(accountId));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to list pending sponsor orders: " + e.getMessage());
        }
    }

    @Tag(name = "/sponsor/" + ApiConstant.LATEST)
    @Operation(summary = "Create sponsor order for current account")
    @PostMapping("/" + ApiConstant.LATEST + "/orders")
    public ResultBody<SponsorOrderDTO> createOrder(@RequestBody SubmitBody<CreateSponsorOrderDTO> submitBody) {
        try {
            int accountId = (int) request.getAttribute("accountId");
            return ResultBody.success(sponsorService.createOrder(accountId, submitBody.getData()));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to create sponsor order: " + e.getMessage());
        }
    }

    @Tag(name = "/sponsor/" + ApiConstant.LATEST)
    @Operation(summary = "Award sponsor order")
    @PostMapping("/" + ApiConstant.LATEST + "/orders/{orderId}/award")
    public ResultBody<SponsorOrderDTO> awardOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody SubmitBody<SponsorAwardDTO> submitBody) {
        try {
            int awardedBy = (int) request.getAttribute("accountId");
            return ResultBody.success(sponsorService.awardOrder(orderId, awardedBy, submitBody.getData()));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to award sponsor order: " + e.getMessage());
        }
    }

    @Tag(name = "/sponsor/" + ApiConstant.LATEST)
    @Operation(summary = "Close unpaid sponsor order")
    @PostMapping("/" + ApiConstant.LATEST + "/orders/{orderId}/close")
    public ResultBody<SponsorOrderDTO> closeOrder(@PathVariable("orderId") String orderId) {
        try {
            int closedBy = (int) request.getAttribute("accountId");
            return ResultBody.success(sponsorService.closeOrder(orderId, closedBy));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Failed to close sponsor order: " + e.getMessage());
        }
    }
}
