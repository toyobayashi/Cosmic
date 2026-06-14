package api.controller;

import api.constant.ApiConstant;
import api.model.ResultBody;
import api.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import provider.Data;
import provider.DataProvider;
import provider.DataProviderFactory;
import provider.wz.BinaryWZMapleData;
import provider.wz.WZFiles;
import tools.StringUtil;

import java.io.OutputStream;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private static final DataProvider mobSource = DataProviderFactory.getDataProvider(WZFiles.MOB);
    private static final DataProvider soundSource = DataProviderFactory.getDataProvider(WZFiles.SOUND);
    private static final DataProvider mapSource = DataProviderFactory.getDataProvider(WZFiles.MAP);

    @Tag(name = "/map/" + ApiConstant.LATEST)
    @Operation(summary = "Get map detail with monsters and BGM")
    @GetMapping("/" + ApiConstant.LATEST + "/{id}")
    public ResultBody<MapService.MapDetail> detail(@PathVariable int id) {
        return ResultBody.success(MapService.getMapDetail(id));
    }

    @Tag(name = "/map/" + ApiConstant.LATEST)
    @Operation(summary = "Get mob stand image")
    @GetMapping(value = "/" + ApiConstant.LATEST + "/mob-image/{mobId}")
    public void mobImage(@PathVariable String mobId, HttpServletResponse resp) {
        try {
            java.io.File cacheDir = new java.io.File("cache/mob-images");
            cacheDir.mkdirs();
            java.io.File pngFile = new java.io.File(cacheDir, mobId + ".png");

            if (!pngFile.exists()) {
                Data mobData = mobSource.getData(mobId + ".img");
                if (mobData == null) { resp.setStatus(404); return; }
                Data s0 = mobData.getChildByPath("stand/0");
                if (s0 instanceof BinaryWZMapleData bwz && bwz.saveCanvasToFile(pngFile)) {
                    resp.sendRedirect("/cache/mob-images/" + mobId + ".png");
                    return;
                } else {
                    resp.setStatus(404);
                    return;
                }
            }

            resp.sendRedirect("/cache/mob-images/" + mobId + ".png");
        } catch (Exception e) {
            resp.setStatus(500);
        }
    }

    @Tag(name = "/map/" + ApiConstant.LATEST)
    @Operation(summary = "Get map BGM audio")
    @GetMapping("/" + ApiConstant.LATEST + "/bgm/{mapId}")
    public void mapBgm(@PathVariable int mapId, HttpServletResponse resp) {
        try {
            String padded = StringUtil.getLeftPaddedStr(Integer.toString(mapId), '0', 9);
            String mapPath = "Map/Map" + (mapId / 100000000) + "/" + padded + ".img";
            Data mapData = mapSource.getData(mapPath);
            if (mapData == null) {
                resp.setStatus(404);
                return;
            }
            Data infoData = mapData.getChildByPath("info");
            if (infoData == null) {
                resp.setStatus(404);
                return;
            }
            String link = provider.DataTool.getString(infoData.getChildByPath("link"), "");
            if (!link.isEmpty()) {
                int linkId = Integer.parseInt(link);
                String linkPadded = StringUtil.getLeftPaddedStr(Integer.toString(linkId), '0', 9);
                String linkPath = "Map/Map" + (linkId / 100000000) + "/" + linkPadded + ".img";
                mapData = mapSource.getData(linkPath);
                if (mapData != null) {
                    infoData = mapData.getChildByPath("info");
                }
            }
            String bgm = provider.DataTool.getString(infoData.getChildByPath("bgm"), "");
            if (bgm.isEmpty()) {
                resp.setStatus(404);
                return;
            }
            // Sound.wz path: "Bgm00/FloralLife" -> "Bgm00.img/FloralLife"
            int slashIdx = bgm.indexOf('/');
            if (slashIdx < 0) {
                resp.setStatus(404);
                return;
            }
            String soundPath = bgm.substring(0, slashIdx) + ".img/" + bgm.substring(slashIdx + 1);
            Data soundData = soundSource.getData(soundPath);
            if (soundData == null) {
                resp.setStatus(404);
                return;
            }
            byte[] audioBytes = getSoundBytes(soundData);
            if (audioBytes == null) {
                resp.setStatus(404);
                return;
            }
            resp.setContentType("audio/mpeg");
            resp.setContentLength(audioBytes.length);
            resp.setHeader("Content-Disposition", "inline");
            try (OutputStream os = resp.getOutputStream()) {
                os.write(audioBytes);
            }
        } catch (Exception e) {
            resp.setStatus(500);
        }
    }

    private static byte[] getSoundBytes(Data data) {
        if (data instanceof BinaryWZMapleData bwz) {
            return bwz.getSoundData();
        }
        return null;
    }
}
