<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="mvc"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link rel="stylesheet" href="/webjars/bootstrap/css/bootstrap.min.css" />
<script src="/webjars/jquery/jquery.min.js"></script>
<script src="/webjars/bootstrap/js/bootstrap.min.js"></script>

<title>Edit Match</title>
</head>
<body style="background-color:#ececec">
	<h2 style="font-family:monospace">Edit Match:</h2>
	<mvc:form modelAttribute="match">
		<table>
			<tr>
				<td><mvc:label path="id">ID</mvc:label></td>
				<td><mvc:input path="id" readOnly="true"/></td>
			</tr>
			<tr>
				<td><mvc:label path="description">Description:</mvc:label></td>
				<td><mvc:input path="description" /></td>
			</tr>
			<tr>
				<td><mvc:label path="active">Active:</mvc:label></td>
				<td>
					<mvc:checkbox path="active"/>      					     				
     			</td>
			</tr>
			<tr>
				<td><a href="/matches" class="btn btn-secondary" style="color:#d9534f">Cancel</a></td>
				<td><input type="submit" value="Save" class="btn btn-danger"/></td>
			</tr>
		</table>
	</mvc:form>
</body>
</html>