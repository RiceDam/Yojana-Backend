package com.yojana.services.project;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.yojana.access.EmployeeManager;
import com.yojana.access.ProjectManager;
import com.yojana.access.WorkPackageManager;
import com.yojana.helpers.ProjectHelper;
import com.yojana.helpers.WorkPackageHelper;
import com.yojana.model.employee.Employee;
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
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
		    return Response.status(Response.Status.FORBIDDEN).entity(res).build();
		}
		project.setProjectManager(employeeManager.find(authEmployee.getId()));
		projectManager.persist(project);
		return Response.created(URI.create("/projects/" + project.getId())).entity(res).build();
	}
	
	@PATCH
	@Path("/{id}")
	@Consumes("application/json")
	@Produces("application/json")
	// Inserts a timesheet ain't the database
	public Response merge(@PathParam("id") String id, Project project) { 
		APIResponse res = new APIResponse();
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
		    return Response.status(Response.Status.FORBIDDEN).entity(res).build();
		}
		Project old = projectManager.find(id);
		ProjectHelper.patchProject(project, old);
		project.setProjectManager(employeeManager.find(authEmployee.getId()));
		projectManager.merge(old);
		return Response.ok().entity(res).build();
	}
	
	@GET
	@Produces("application/json")
	// Gets a list of all timesheets
	public Response getAll() {
		final APIResponse res = new APIResponse();
		if(!authEmployee.isAdmin()) {
            return Response.status(Response.Status.FORBIDDEN).entity(res).build();
        }
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
	// Finds a project
	public Response find(@PathParam("id") String id) {
		Project project = projectManager.find(id);
		APIResponse res = new APIResponse();
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
            return Response.status(Response.Status.FORBIDDEN).entity(res).build();
        }
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
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
            return Response.status(Response.Status.FORBIDDEN).entity(res).build();
        }
		Project project = projectManager.find(projectId);
		wp.setProject(project);
		
		wpManager.persist(wp);
		res.getData().put("id", wp.getWorkPackagePk().getId());
		return Response.created(URI.create("/projects/" + projectId + "/workPackages/" + wp.getWorkPackagePk()
			.getId()))
			.entity(res)
			.build();
	}
	
	@PATCH
	@Path("/{id}/workPackages/{wpId}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response modifyWP(@PathParam("id") String projectId,
			@PathParam("wpId") String wpId,
			WorkPackage wp) {
		APIResponse res = new APIResponse();
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
            return Response.status(Response.Status.FORBIDDEN).entity(res).build();
        }
		WorkPackage old = wpManager.find(new WorkPackagePK(wpId, projectId));
		if (old == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundSingle("workPackage",
					wpId,
					null
					));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		WorkPackageHelper.patchWorkPackage(wp, old);
		Project project = projectManager.find(projectId);
		old.setProject(project);
		
		wpManager.merge(old);
		return Response.ok()
			.entity(res)
			.build();
	}
	
	@GET
	@Path("/{id}/workPackages")
	@Produces("application/json")
	public Response getWorkPackages(@PathParam("id") String projectId,
			@QueryParam("hierarchyLevel") Integer hierarchyLevel) {
		APIResponse res = new APIResponse();
		List<WorkPackage> wps;
		if (hierarchyLevel == null) {
			wps = wpManager.getAll(projectId);
		} else {
			wps = wpManager.getAllWithHierarchyLevel(projectId, hierarchyLevel);
		}
		res.getData().put("workPackages", wps);
		return Response.ok().entity(res).build();
	}
	
	@GET
	@Path("/{id}/workPackages/{wpId}/children")
	@Produces("application/json")
	public Response getChildWPs(@PathParam("id") String projectId, 
			@PathParam("wpId") String wpId) {
		APIResponse res = new APIResponse();
		List<WorkPackage> wps = wpManager.getChildWPs(projectId, wpId);
		res.getData().put("workPackages", wps);
		return Response.ok().entity(res).build();
	}
	
	@GET
	@Path("/{id}/workPackages/{wpId}")
	@Produces("application/json")
	public Response getWorkPackage(
			@PathParam("id") String projectId, 
			@PathParam("wpId") String wpId
	) {
		APIResponse res = new APIResponse();
		if(!authEmployee.isAdmin() && !authEmployee.isProjectManager()) {
            return Response.status(Response.Status.FORBIDDEN).entity(res).build();
        }
		WorkPackage wp = wpManager.find(new WorkPackagePK(wpId, projectId));
		res.getData().put("workPackage", wp);
		return Response.ok().entity(res).build();
	}
}
