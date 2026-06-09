package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.model.dto.GiveResourceDTO;
import api.service.GiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/give")
public class GiveController {

    private static final GiveService giveService = new GiveService();

    @Tag(name = "/give/" + ApiConstant.LATEST)
    @Operation(summary = "Give resource to player or globally")
    @PostMapping("/" + ApiConstant.LATEST + "/resource")
    public ResultBody<Object> giveResource(@RequestBody SubmitBody<GiveResourceDTO> submitBody) {
        try {
            giveService.giveResource(submitBody.getData());
            return ResultBody.success();
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        } catch (Exception e) {
            return ResultBody.error(500, "Give failed: " + e.getMessage());
        }
    }
}
