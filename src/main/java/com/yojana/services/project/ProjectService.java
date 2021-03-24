package com.yojana.services.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.yojana.access.EmployeeManager;
import com.yojana.access.EstimateManager;
import com.yojana.access.ProjectManager;
import com.yojana.access.WorkPackageManager;
import com.yojana.model.employee.Employee;
import com.yojana.model.project.Estimate;
import com.yojana.model.project.Project;
import com.yojana.model.project.WorkPackage;
import com.yojana.model.project.WorkPackagePK;
import com.yojana.response.APIResponse;
import com.yojana.response.errors.ErrorMessageBuilder;
import com.yojana.security.annotations.AuthenticatedEmployee;
import com.yojana.security.annotations.Secured;

@Path("/projects")
@Secured
public class ProjectService {
	
	@Inject
	// Provides access to the employee table
	private EmployeeManager employeeManager;
	
	@Inject
	// Provides access to the project table
	private ProjectManager projectManager;
	
	@Inject
	private WorkPackageManager wpManager;
	
	@Inject
    @AuthenticatedEmployee
    // Gets the authenticated employee
    private Employee authEmployee;

	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	// Inserts a timesheet ain't the database
	public Response persist(Project project) {
		APIResponse res = new APIResponse();
		project.setProjectManager(employeeManager.find(authEmployee.getId()));
		projectManager.persist(project);
		return Response.created(URI.create("/projects/" + project.getId())).entity(res).build();
	}
	
	@GET
	@Produces("application/json")
	// Gets a list of all timesheets
	public Response getAll() {
		final APIResponse res = new APIResponse();
		List<Project> projects = projectManager.getAll();
		if (projects == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundMultiple("project", null));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		res.getData().put("projects", projects);
		return Response.ok().entity(res).build();
	}

	@GET
    @Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	// Finds an employee
	public Response find(@PathParam("id") String id) {
		Project project = projectManager.find(id);
		APIResponse res = new APIResponse();
		if (project == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundSingle("project",
					id + "",
					null
					));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
        res.getData().put("project", project);
        return Response.ok().entity(res).build();
	}
	
	@POST
	@Path("/{id}/workPackages")
	@Consumes("application/json")
	@Produces("application/json")
	public Response addWP(@PathParam("id") String projectId, WorkPackage wp) {
		APIResponse res = new APIResponse();
		Project project = projectManager.find(projectId);
		wp.setProject(project);
		
		wpManager.persist(wp);
		return Response.created(URI.create("/projects/" + projectId + "/workPackages/" + wp.getWorkPackagePk()
			.getId()))
			.entity(res)
			.build();
	}
	
	@GET
	@Path("/{id}/workPackages")
	@Produces("application/json")
	public Response getWorkPackages(@PathParam("id") String projectId) {
		APIResponse res = new APIResponse();
		List<WorkPackage> wps = wpManager.getAll(projectId);
		res.getData().put("workPackages", wps);
		return Response.ok().entity(res).build();
	}
	
	@GET
	@Path("/{id}/workPackages/{wpId}")
	@Produces("application/json")
	public Response getWorkPackages(@PathParam("id") String projectId, @PathParam("wpId") String wpId) {
		APIResponse res = new APIResponse();
		WorkPackage wp = wpManager.find(new WorkPackagePK(wpId, projectId));
		res.getData().put("workPackage", wp);
		return Response.ok().entity(res).build();
	}
	
	@Inject
    private EstimateManager estimateManager;
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response persist(Estimate estimate) {
        APIResponse res = new APIResponse();
        UUID uuid = UUID.randomUUID();
        estimate.setEstimateId(uuid);
        estimateManager.persist(estimate);
        return Response.created(URI.create("/estimates/" + estimate.getEstimateId())).entity(res).build();
    }
    
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response merge(@PathParam("id") UUID id, Estimate estimate) {
        APIResponse res = new APIResponse();
        if (!id.equals(estimate.getEstimateId())) {
            res.getErrors().add(ErrorMessageBuilder.badRequest(
                    "EstimateID in route doesn't match estimate id in body", null));
        }
        final Estimate old = estimateManager.find(id);
        if (old == null) {
            res.getErrors().add(ErrorMessageBuilder.notFoundSingle("estimate", id.toString(), null));
            return Response.status(Response.Status.NOT_FOUND).entity(res).build();
        }
        estimateManager.merge(estimate);
        return Response.ok().entity(new APIResponse()).build();
    }
    
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@PathParam("id") UUID id) {
        final APIResponse res = new APIResponse();
        estimateManager.remove(id);
        return Response.ok().entity(res).build();
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("id") UUID id) {
        Estimate estimate = estimateManager.find(id);
        APIResponse res = new APIResponse();
        if (estimate == null) {
            res.getErrors().add(ErrorMessageBuilder.notFoundSingle(
                    "estimate", ""+id, null));
            return Response.status(Response.Status.NOT_FOUND).entity(res).build();
        }
        res.getData().put("estimate", estimate);
        return Response.ok().entity(res).build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllForWorkPackage() {
        final APIResponse res = new APIResponse();
        List<Estimate> estimates = estimateManager.getAll();
        if (estimates == null) {
            res.getErrors().add(ErrorMessageBuilder.notFoundMultiple("estimate", null));
            return Response.status(Response.Status.NOT_FOUND).entity(res).build();
        }
        res.getData().put("estimates", estimates);
        return Response.ok().entity(res).build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllForProject() {
        final APIResponse res = new APIResponse();
        List<Estimate> estimates = estimateManager.getAll();
        if (estimates == null) {
            res.getErrors().add(ErrorMessageBuilder.notFoundMultiple("estimate", null));
            return Response.status(Response.Status.NOT_FOUND).entity(res).build();
        }
        res.getData().put("estimates", estimates);
        return Response.ok().entity(res).build();
    }
}
