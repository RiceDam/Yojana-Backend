package com.yojana.services.timesheet;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.yojana.access.EmployeeManager;
import com.yojana.access.ProjectManager;
import com.yojana.access.TimesheetManager;
import com.yojana.access.TimesheetRowManager;
import com.yojana.access.WorkPackageManager;
import com.yojana.model.employee.Employee;
import com.yojana.model.project.WorkPackagePK;
import com.yojana.model.timesheet.Timesheet;
import com.yojana.model.timesheet.TimesheetRow;
import com.yojana.model.timesheet.TimesheetStatus;
import com.yojana.response.APIResponse;
import com.yojana.response.errors.ErrorMessageBuilder;
import com.yojana.security.annotations.AuthenticatedEmployee;
import com.yojana.security.annotations.Secured;

@Path("/timesheets")
@Secured
public class TimesheetService {

	@Inject
	private ProjectManager projectManager;

	@Inject
	// Provides access to the employee table
	private TimesheetManager timesheetManager;

	@Inject
	private TimesheetRowManager timesheetRowManager;

	@Inject
	private WorkPackageManager workPackageManager;

	@Inject
	// Provides access to the employee table
	private EmployeeManager employeeManager;

	@Inject
	@AuthenticatedEmployee
	private Employee authEmployee;

	@GET
	@Path("/{id}")
	@Produces("application/json")
	// Finds a timesheet
	public Response find(@PathParam("id") UUID id) {
		Timesheet timesheet = timesheetManager.find(id);
		APIResponse res = new APIResponse();
		if (timesheet == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundSingle("timesheet", id.toString(), null));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		res.getData().put("timesheet", timesheet);
		return Response.ok().entity(res).build();
	}

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	// Inserts a timesheet ain't the database
	public Response persist(Timesheet timesheet) {
		APIResponse res = new APIResponse();
		timesheet.setOwnerId(authEmployee.getId());
		if (timesheet.getOwnerId() > 0) {
			final Employee emp = employeeManager.find(timesheet.getOwnerId());
			if (emp == null) {
				ErrorMessageBuilder.notFound("Could not find employee for timesheet", null);
				return Response.status(Response.Status.NOT_FOUND).entity(res).build();
			}
			timesheet.setEmployee(emp);
		}
		UUID uuid = UUID.randomUUID();
		timesheet.setId(uuid);
		timesheetManager.persist(timesheet);
		res.getData().put("id", timesheet.getId());
		return Response.created(URI.create("/timesheets/" + timesheet.getId())).entity(res).build();
	}

	@PATCH
	@Path("/{id}")
	@Consumes("application/json")
	@Produces("application/json")
	// Updates a existing timesheet
	public Response merge(@PathParam("id") UUID id, Timesheet timesheet) {
		APIResponse res = new APIResponse();
		if (!id.equals(timesheet.getId())) {
			res.getErrors().add(
					ErrorMessageBuilder.badRequest("TimesheetID in route doesn't match timesheet id in body", null));
			return Response.status(Response.Status.BAD_REQUEST).entity(res).build();
		}
		final Timesheet old = timesheetManager.find(id);
		if (old == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundSingle("timesheet", id.toString(), null));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		if (timesheet.getOwnerId() > 0) {
			old.setEmployee(employeeManager.find(timesheet.getOwnerId()));
		}
		old.setId(timesheet.getId());
		old.setAudit(timesheet.getAudit());
		old.setEmployee(employeeManager.find(timesheet.getOwnerId()));
		old.setOwnerId(timesheet.getOwnerId());
		old.setEndWeek(timesheet.getEndWeek());
		old.setReviewer(timesheet.getReviewer());
		old.setReviewerId(timesheet.getReviewerId());
		old.setSignature(timesheet.getSignature());
		old.setFeedback(timesheet.getFeedback());
		old.setOvertime(timesheet.getOvertime());
		old.setFlextime(timesheet.getFlextime());
		old.setApprovedAt(timesheet.getApprovedAt());
		old.setTimesheetRows(timesheet.getTimesheetRows());
		old.setStatus(TimesheetStatus.valueOf(timesheet.getStatus()));
		timesheetManager.merge(old);
		return Response.ok().entity(res).build();
	}

	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	// Deletes a existing timesheet
	public Response remove(@PathParam("id") UUID id) {
		final APIResponse res = new APIResponse();
		timesheetManager.remove(id);
		return Response.ok().entity(res).build();
	}

	@GET
	@Produces("application/json")
	// Gets a list of all timesheets
	public Response getAll(@QueryParam("status") String status, @QueryParam("getAll") Boolean getAll,
			@QueryParam("empId") Integer empId) {
		final APIResponse res = new APIResponse();
		List<Timesheet> timesheets = null;
		if (status != null && status.equals("submitted") && getAll != null && getAll) {
			timesheets = timesheetManager.getAllSubmittedTimesheets();
		} else if (status != null && status.equals("submitted")) {
			timesheets = timesheetManager.getAllSubmittedTimesheetsForApprover(authEmployee.getId());
		} else if (empId != null) {
			timesheets = timesheetManager.getAllForEmployee(empId);
		} else {
			timesheets = timesheetManager.getAll();
		}
		if (timesheets == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundMultiple("timesheet", null));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		res.getData().put("timesheets", timesheets);
		return Response.ok().entity(res).build();
	}

	@POST
	@Path("/{id}/rows")
	@Consumes("application/json")
	@Produces("application/json")
	// Inserts a timesheetrow into the database
	public Response addRow(@PathParam("id") UUID timesheetId, TimesheetRow row) {
		final APIResponse res = new APIResponse();

		row.setTimesheetId(timesheetId);
		row.setTimesheet(timesheetManager.find(timesheetId));
		row.setProject(projectManager.find(row.getProjectId()));
		row.setWorkPackage(workPackageManager.find(new WorkPackagePK(row.getWorkPackageId(), row.getProjectId())));

		timesheetRowManager.persist(row);
		return Response.created(URI.create("/timesheets/" + row.getTimesheetId() + "/rows/" + "project/"
				+ row.getProjectId() + "/wp/" + row.getWorkPackageId())).entity(res).build();
	}

	@PUT
	@Path("/{id}/rows")
	@Consumes("application/json")
	@Produces("application/json")
	// Updates a timesheetrow from the database
	public Response updateRow(@PathParam("id") UUID timesheetId, TimesheetRow row) {
		final APIResponse res = new APIResponse();

		TimesheetRow old = timesheetRowManager.find(timesheetId, row.getIndex());
		boolean isNewRow = false;
		if (old == null) {
			old = new TimesheetRow();
			isNewRow = true;
		}

		old.setProject(projectManager.find(row.getProjectId()));
		old.setWorkPackage(workPackageManager.find(new WorkPackagePK(row.getWorkPackageId(), row.getProjectId())));

		old.setHours(row.getHours());
		old.setIndex(row.getIndex());
		old.setNotes(row.getNotes());
		old.setProjectId(row.getProjectId());
		old.setTimesheet(timesheetManager.find(timesheetId));
		old.setTimesheetId(timesheetId);
		old.setWorkPackageId(row.getWorkPackageId());

		if (isNewRow) {
			timesheetRowManager.persist(old);
		} else {
			timesheetRowManager.merge(old);
		}
		return Response.created(URI.create("/timesheets/" + row.getTimesheetId() + "/rows/" + "project/"
				+ row.getProjectId() + "/wp/" + row.getWorkPackageId())).entity(res).build();
	}

	@GET
	@Path("/{id}/rows")
	@Produces("application/json")
	// Gets a list of all timesheetrows for a timesheet
	public Response getAllForTimesheet(@PathParam("id") UUID timesheetId) {
		APIResponse res = new APIResponse();
		List<TimesheetRow> timesheetRows = timesheetRowManager.getAllForTimesheet(timesheetId);
		if (timesheetRows == null) {
			res.getErrors().add(ErrorMessageBuilder.notFoundMultiple("timesheetRows",
					"No rows found for" + "timesheet: " + timesheetId));
			return Response.status(Response.Status.NOT_FOUND).entity(res).build();
		}
		res.getData().put("timesheetRows", timesheetRows);
		return Response.ok().entity(res).build();
	}

}
