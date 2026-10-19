package com.traceusage.traceusage.environment;

import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applications/{projectId}/environments")
public class EnvironmentController {
    private final JdbcTemplate jdbc;
    public EnvironmentController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public ApiResponse<List<EnvironmentResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable String projectId) {
        Long applicationId = jdbc.query("select id from applications where project_id = ? and owner_id = ?",
                (rs, row) -> rs.getLong(1), projectId, user.id()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ApiResponse.success("Environments retrieved", jdbc.query(
                "select id, name from environments where application_id = ? order by name",
                (rs, row) -> new EnvironmentResponse(rs.getLong(1), rs.getString(2)), applicationId));
    }
    public record EnvironmentResponse(Long id, String name) { }
}
