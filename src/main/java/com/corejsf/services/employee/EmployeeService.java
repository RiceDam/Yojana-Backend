package com.corejsf.services.employee;

import java.net.URI;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.corejsf.access.EmployeeManager;
import com.corejsf.model.employee.Employee;

@Path("/employees")
public class EmployeeService {

	@Inject
	private EmployeeManager employeeManager;
	
	@GET
    @Path("/{id}")
    @Produces("application/xml")
	public Employee find(@PathParam("id") Integer id) throws SQLException {
		Employee employee;
		employee = employeeManager.find(id);
		if (employee == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		return employee;
	}
	
	@POST
    @Consumes("application/xml")
	public Response persist(Employee employee) throws SQLException {
		employeeManager.persist(employee);
		return Response.created(URI.create("/employees/" + employee.getUsername())).build();
	}
	
//	@PUT
//    @Path("update-employee")
	@PATCH
	@Path("/{id}")
    @Consumes("application/xml")
	public Response merge(Employee employee,@PathParam("id") Integer id) throws SQLException {
		employeeManager.merge(employee, id);
		return Response.noContent().build();
	}
	
	@DELETE
	@Path("/{id}")
	public Response remove(Employee employee, @PathParam("id") Integer empId) throws SQLException {
		employeeManager.remove(employee, empId);
		return Response.ok().build();
	}
	
	@GET
    @Path("all")
    @Produces("application/xml")
	public Employee[] getAll() throws SQLException {
		Employee[] employees;
		employees = employeeManager.getAll();
		if (employees == null) {
			throw new WebApplicationException("There are no employees at the momnet", Response.Status.NOT_FOUND);
		}
		return employees;
	}
}