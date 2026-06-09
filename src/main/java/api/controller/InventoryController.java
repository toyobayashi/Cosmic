package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.model.dto.InventoryItemDTO;
import api.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final InventoryService inventoryService = new InventoryService();

    @Tag(name = "/inventory/" + ApiConstant.LATEST)
    @Operation(summary = "Get inventory types")
    @GetMapping("/" + ApiConstant.LATEST + "/types")
    public ResultBody<List<Map<String, Object>>> getTypes() {
        return ResultBody.success(inventoryService.getInventoryTypes());
    }

    @Tag(name = "/inventory/" + ApiConstant.LATEST)
    @Operation(summary = "Get character list for inventory search")
    @GetMapping("/" + ApiConstant.LATEST + "/characters")
    public ResultBody<List<Map<String, Object>>> getCharacters(@RequestParam(name = "keyword", required = false) String keyword) {
        return ResultBody.success(inventoryService.getCharacterList(keyword));
    }

    @Tag(name = "/inventory/" + ApiConstant.LATEST)
    @Operation(summary = "Get inventory item list")
    @GetMapping("/" + ApiConstant.LATEST)
    public ResultBody<Map<String, Object>> getList(
            @RequestParam(name = "characterId", required = false) Integer characterId,
            @RequestParam(name = "characterName", required = false) String characterName,
            @RequestParam(name = "accountId", required = false) Integer accountId,
            @RequestParam(name = "inventoryType", required = false) Integer inventoryType,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        if (characterId == null && characterName == null && accountId == null) {
            return ResultBody.error(400, "characterId, characterName or accountId is required");
        }
        List<Map<String, Object>> list = inventoryService.getInventoryList(characterId, characterName, accountId, inventoryType, page, size);
        long total = inventoryService.getInventoryCount(characterId, characterName, accountId, inventoryType);
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

    @Tag(name = "/inventory/" + ApiConstant.LATEST)
    @Operation(summary = "Update inventory item")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> update(@PathVariable("id") int id,
                                      @RequestBody SubmitBody<InventoryItemDTO> submitBody) {
        inventoryService.updateInventoryItem(id, submitBody.getData());
        return ResultBody.success();
    }

    @Tag(name = "/inventory/" + ApiConstant.LATEST)
    @Operation(summary = "Delete inventory item")
    @DeleteMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> delete(@PathVariable("id") int id) {
        inventoryService.deleteInventoryItem(id);
        return ResultBody.success();
    }
}
