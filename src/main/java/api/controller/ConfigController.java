package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final ConfigService configService = new ConfigService();

    @Tag(name = "/config/" + ApiConstant.LATEST)
    @Operation(summary = "Get config type list")
    @GetMapping("/" + ApiConstant.LATEST + "/types")
    public ResultBody<Map<String, Object>> getTypes() {
        return ResultBody.success(configService.getConfigTypes());
    }

    @Tag(name = "/config/" + ApiConstant.LATEST)
    @Operation(summary = "Get config list")
    @GetMapping("/" + ApiConstant.LATEST)
    public ResultBody<Map<String, Object>> getList(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "subType", required = false) String subType,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return ResultBody.success(configService.getConfigList(type, subType, filter, page, size));
    }

    @Tag(name = "/config/" + ApiConstant.LATEST)
    @Operation(summary = "Update config value")
    @PutMapping("/" + ApiConstant.LATEST)
    public ResultBody<Object> update(@RequestBody SubmitBody<Map<String, Object>> submitBody) {
        try {
            Map<String, Object> data = submitBody.getData();
            String configType = (String) data.get("configType");
            String configSubType = (String) data.get("configSubType");
            String configCode = (String) data.get("configCode");
            String configValue = (String) data.get("configValue");

            boolean ok = configService.updateConfig(configType, configSubType, configCode, configValue);
            if (ok) {
                return ResultBody.success();
            } else {
                return ResultBody.error(400, "Config not found or update failed");
            }
        } catch (Exception e) {
            return ResultBody.error(500, e.getMessage());
        }
    }

    @Tag(name = "/config/" + ApiConstant.LATEST)
    @Operation(summary = "Save config to disk")
    @PostMapping("/" + ApiConstant.LATEST + "/save")
    public ResultBody<Object> saveToDisk() {
        try {
            configService.saveConfigToDisk();
            return ResultBody.success();
        } catch (Exception e) {
            return ResultBody.error(500, e.getMessage());
        }
    }
}
