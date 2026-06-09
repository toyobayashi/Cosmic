package api.controller;

import api.config.JwtUtil;
import api.model.ResultBody;
import api.model.SubmitBody;
import api.model.dto.LoginDTO;
import api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Tag(name = "Auth")
    @Operation(summary = "Login - returns JWT token")
    @PostMapping("/login")
    public ResultBody<Map<String, String>> login(@RequestBody SubmitBody<LoginDTO> submitBody) {
        try {
            AuthService authService = new AuthService(jwtUtil);
            String token = authService.login(
                    submitBody.getData().getUsername(),
                    submitBody.getData().getPassword()
            );
            return ResultBody.success(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResultBody.error(400, e.getMessage());
        }
    }

    @Tag(name = "Auth")
    @Operation(summary = "Get current user info")
    @GetMapping("/info")
    public ResultBody<Map<String, Object>> info() {
        Integer accountId = (Integer) request.getAttribute("accountId");
        if (accountId == null) {
            return ResultBody.error(401, "Not authenticated");
        }
        return ResultBody.success(Map.of("accountId", accountId));
    }
}
