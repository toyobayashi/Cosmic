package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private static final ShopService shopService = new ShopService();

    @Tag(name = "/shop/" + ApiConstant.LATEST)
    @Operation(summary = "Get shop list")
    @GetMapping("/" + ApiConstant.LATEST + "/list")
    public ResultBody<List<Map<String, Object>>> getShopList(
            @RequestParam(required = false) Integer shopId,
            @RequestParam(required = false) Integer npcId) {
        return ResultBody.success(shopService.getShopList(shopId, npcId, null, null));
    }

    @Tag(name = "/shop/" + ApiConstant.LATEST)
    @Operation(summary = "Get shop items")
    @GetMapping("/" + ApiConstant.LATEST + "/items")
    public ResultBody<List<Map<String, Object>>> getShopItems(@RequestParam int shopId) {
        return ResultBody.success(shopService.getShopItems(shopId));
    }

    @Tag(name = "/shop/" + ApiConstant.LATEST)
    @Operation(summary = "Add shop item")
    @PostMapping("/" + ApiConstant.LATEST + "/item")
    public ResultBody<Object> addShopItem(@RequestBody Map<String, Object> body) {
        shopService.addShopItem(
                (int) body.get("shopId"),
                (int) body.get("itemId"),
                (int) body.getOrDefault("price", 0),
                (int) body.getOrDefault("pitch", 0),
                (int) body.getOrDefault("position", 0)
        );
        return ResultBody.success();
    }

    @Tag(name = "/shop/" + ApiConstant.LATEST)
    @Operation(summary = "Update shop item")
    @PutMapping("/" + ApiConstant.LATEST + "/item/{id}")
    public ResultBody<Object> updateShopItem(@PathVariable int id, @RequestBody Map<String, Object> body) {
        shopService.updateShopItem(id,
                body.containsKey("itemId") ? ((Number) body.get("itemId")).intValue() : null,
                body.containsKey("price") ? ((Number) body.get("price")).intValue() : null,
                body.containsKey("pitch") ? ((Number) body.get("pitch")).intValue() : null,
                body.containsKey("position") ? ((Number) body.get("position")).intValue() : null
        );
        return ResultBody.success();
    }

    @Tag(name = "/shop/" + ApiConstant.LATEST)
    @Operation(summary = "Delete shop item")
    @DeleteMapping("/" + ApiConstant.LATEST + "/item/{id}")
    public ResultBody<Object> deleteShopItem(@PathVariable int id) {
        shopService.deleteShopItem(id);
        return ResultBody.success();
    }
}
