package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.model.dto.AccountInfoDTO;
import api.model.dto.AddAccountDTO;
import api.model.dto.UpdateAccountByGmDTO;
import api.model.dto.UpdateAccountByUserDTO;
import api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private HttpServletRequest request;

    private static final AccountService accountService = new AccountService();

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Get current account info")
    @GetMapping("/" + ApiConstant.LATEST + "/info")
    public ResultBody<AccountInfoDTO> info() {
        int accountId = (int) request.getAttribute("accountId");
        return ResultBody.success(accountService.getAccountById(accountId));
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Get account list")
    @GetMapping("/" + ApiConstant.LATEST)
    public ResultBody<Map<String, Object>> getAccountList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "lastLoginStart", required = false) String lastLoginStart,
            @RequestParam(name = "lastLoginEnd", required = false) String lastLoginEnd,
            @RequestParam(name = "createdAtStart", required = false) String createdAtStart,
            @RequestParam(name = "createdAtEnd", required = false) String createdAtEnd) {

        List<AccountInfoDTO> list = accountService.getAccountList(
                page, size, id, name, lastLoginStart, lastLoginEnd, createdAtStart, createdAtEnd);
        long total = accountService.getAccountCount(
                id, name, lastLoginStart, lastLoginEnd, createdAtStart, createdAtEnd);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        if (page != null && size != null && size > 0) {
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
        }
        return ResultBody.success(result);
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Register account")
    @PostMapping("/" + ApiConstant.LATEST)
    public ResultBody<Object> register(@RequestBody SubmitBody<AddAccountDTO> submitBody) {
        try {
            accountService.addAccount(submitBody.getData());
            return ResultBody.success();
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        }
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Update account (user self-service)")
    @PutMapping("/" + ApiConstant.LATEST)
    public ResultBody<Object> updateByUser(@RequestBody SubmitBody<UpdateAccountByUserDTO> submitBody) {
        try {
            AccountService.setCurrentUserId((int) request.getAttribute("accountId"));
            accountService.updateAccountByUser(submitBody.getData());
            return ResultBody.success();
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        }
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Update account (GM)")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> updateByGm(@PathVariable("id") int id,
                                          @RequestBody SubmitBody<UpdateAccountByGmDTO> submitBody) {
        try {
            accountService.updateAccountByGM(id, submitBody.getData());
            return ResultBody.success();
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        }
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Delete account")
    @DeleteMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> delete(@PathVariable("id") int id) {
        accountService.deleteAccount(id);
        return ResultBody.success();
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Reset login status")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}/reset/logged")
    public ResultBody<Object> resetLoggedIn(@PathVariable("id") int id) {
        accountService.resetLoggedIn(id);
        return ResultBody.success();
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Ban account")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}/ban")
    public ResultBody<Object> banAccount(@PathVariable("id") int id,
                                          @RequestBody SubmitBody<Map<String, String>> submitBody) {
        accountService.banAccount(id, submitBody.getData().get("reason"));
        return ResultBody.success();
    }

    @Tag(name = "/account/" + ApiConstant.LATEST)
    @Operation(summary = "Unban account")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}/unban")
    public ResultBody<Object> unbanAccount(@PathVariable("id") int id) {
        accountService.unbanAccount(id);
        return ResultBody.success();
    }
}
