package org.apache.cassandra.sidecar.routes;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * HealthService API
 */
public interface IHealthService
{
    @Operation(summary = "Health Check for Cassandra's status",
    description = "Returns HTTP 200 if Cassandra is available, 503 otherwise",
    responses =
    {
    @ApiResponse(responseCode = "200", description = "Cassandra is available"),
    @ApiResponse(responseCode = "503", description = "Cassandra is not available")
    })
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response doGet();
}
