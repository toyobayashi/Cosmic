package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.service.DropService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drop")
public class DropController {

    private static final DropService dropService = new DropService();

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Get drop list")
    @GetMapping("/" + ApiConstant.LATEST + "/list")
    public ResultBody<Map<String, Object>> getDropList(
            @RequestParam(required = false) Integer dropperId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = dropService.getDropList(dropperId, itemId, page, size);
        long total = dropService.getDropCount(dropperId, itemId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ResultBody.success(result);
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Add drop data")
    @PostMapping("/" + ApiConstant.LATEST)
    public ResultBody<Object> addDropData(@RequestBody Map<String, Object> body) {
        dropService.addDropData(
                ((Number) body.get("dropperId")).intValue(),
                ((Number) body.get("itemId")).intValue(),
                ((Number) body.getOrDefault("chance", 0)).intValue(),
                ((Number) body.getOrDefault("minQty", 1)).intValue(),
                ((Number) body.getOrDefault("maxQty", 1)).intValue(),
                ((Number) body.getOrDefault("questId", 0)).intValue()
        );
        return ResultBody.success();
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Update drop data")
    @PutMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> updateDropData(@PathVariable int id, @RequestBody Map<String, Object> body) {
        dropService.updateDropData(id,
                body.containsKey("dropperId") ? ((Number) body.get("dropperId")).intValue() : null,
                body.containsKey("itemId") ? ((Number) body.get("itemId")).intValue() : null,
                body.containsKey("chance") ? ((Number) body.get("chance")).intValue() : null,
                body.containsKey("minQty") ? ((Number) body.get("minQty")).intValue() : null,
                body.containsKey("maxQty") ? ((Number) body.get("maxQty")).intValue() : null,
                body.containsKey("questId") ? ((Number) body.get("questId")).intValue() : null
        );
        return ResultBody.success();
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Delete drop data")
    @DeleteMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<Object> deleteDropData(@PathVariable int id) {
        dropService.deleteDropData(id);
        return ResultBody.success();
    }

    // Global drops
    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Get global drop list")
    @GetMapping("/" + ApiConstant.LATEST + "/global/list")
    public ResultBody<Map<String, Object>> getGlobalDropList(
            @RequestParam(required = false) Integer continent,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> list = dropService.getGlobalDropList(continent, itemId, page, size);
        long total = dropService.getGlobalDropCount(continent, itemId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ResultBody.success(result);
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Add global drop data")
    @PostMapping("/" + ApiConstant.LATEST + "/global")
    public ResultBody<Object> addGlobalDropData(@RequestBody Map<String, Object> body) {
        dropService.addGlobalDropData(
                ((Number) body.get("continent")).intValue(),
                ((Number) body.get("itemId")).intValue(),
                ((Number) body.getOrDefault("chance", 0)).intValue(),
                ((Number) body.getOrDefault("minQty", 1)).intValue(),
                ((Number) body.getOrDefault("maxQty", 1)).intValue(),
                ((Number) body.getOrDefault("questId", 0)).intValue(),
                (String) body.getOrDefault("comments", null)
        );
        return ResultBody.success();
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Update global drop data")
    @PutMapping("/" + ApiConstant.LATEST + "/global/{id}")
    public ResultBody<Object> updateGlobalDropData(@PathVariable int id, @RequestBody Map<String, Object> body) {
        dropService.updateGlobalDropData(id,
                body.containsKey("continent") ? ((Number) body.get("continent")).intValue() : null,
                body.containsKey("itemId") ? ((Number) body.get("itemId")).intValue() : null,
                body.containsKey("chance") ? ((Number) body.get("chance")).intValue() : null,
                body.containsKey("minQty") ? ((Number) body.get("minQty")).intValue() : null,
                body.containsKey("maxQty") ? ((Number) body.get("maxQty")).intValue() : null,
                body.containsKey("questId") ? ((Number) body.get("questId")).intValue() : null,
                body.containsKey("comments") ? (String) body.get("comments") : null
        );
        return ResultBody.success();
    }

    @Tag(name = "/drop/" + ApiConstant.LATEST)
    @Operation(summary = "Delete global drop data")
    @DeleteMapping("/" + ApiConstant.LATEST + "/global/{id}")
    public ResultBody<Object> deleteGlobalDropData(@PathVariable int id) {
        dropService.deleteGlobalDropData(id);
        return ResultBody.success();
    }
}
