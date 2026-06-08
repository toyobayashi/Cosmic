package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/command")
public class CommandController {

    private static final CommandService commandService = new CommandService();

    @Tag(name = "/command/" + ApiConstant.LATEST)
    @Operation(summary = "Get command list")
    @GetMapping("/" + ApiConstant.LATEST)
    public ResultBody<Map<String, Object>> getList(
            @RequestParam(name = "level", required = false) Integer level,
            @RequestParam(name = "syntax", required = false) String syntax,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return ResultBody.success(commandService.getCommandList(level, syntax, page, size));
    }
}
