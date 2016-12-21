<!DOCTYPE html>
<html>
	<head>
		<title>${class.className}</title>
		<meta charset="utf-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<!--[if lte IE 8]><script src="js/ie/html5shiv.js"></script><![endif]-->
		<link rel="stylesheet" href="css/main.css" />
		<!--[if lte IE 9]><link rel="stylesheet" href="css/ie9.css" /><![endif]-->
		<!--[if lte IE 8]><link rel="stylesheet" href="css/ie8.css" /><![endif]-->
	</head>
<body>
	<header id="header">
		<a href="/" class="title">PESH CS</a>
		<nav>
			<ul>
				<li><a href="/">Home</a></li>
				<li><a href="/class/classID" class="active">Class</a></li>
			</ul>
		</nav>
	</header>

	<div id="wrapper">
		<section id="main" class="wrapper">
			<div class="inner">
				<h1 class="major js-name">${class.className}</h1>
				<section>
					<h2>Info</h2>
					<p>
						Access Code: <code>${class.classID}</code><br>
						Number of Assignments: ${class.assignLength}<br>
						Joined Students: ${class.joinedLength}<br>
						Invited Students: ${class.invitedLength}<br><hr />
					</p>
				</section>
				<section>
					<h2>Update Name</h2>
					<form method="post" action="#" id="updateName">
						<div class="row uniform">
							<div class="12u$">
								<input type="text" name="class-name" id="class-name" value="" placeholder="Class Name" />
							</div>
							<div class="12u$">
								<ul class="actions">
									<li><input type="submit" value="Update" class="special" /></li>
								</ul>
							</div>
						</div>
					</form>
					<hr />
				</section>
				<section>
					<h2>Invited Students</h2>
					<ul class="alt">
						<li>Student Name | Student Code: <code>q20cj2</code></li>
						<li>Student Name | Student Code: <code>q20cj2</code></li>
					</ul>
					<h3>Invite New Student</h3>
					<form method="post" action="#">
						<div class="row uniform">
							<div class="8u 12u$(medium)">
								<input type="text" name="student-name" id="student-name" value="" placeholder="Student Name" />
							</div>
							<div class="4u 12u$(medium)">
								<ul class="actions">
									<li><input type="submit" value="Add Student" class="special"/></li>
								</ul>
							</div>
						</div>
					</form>
					<hr />
				</section>
				<section>
					<h2>Joined Students</h2>
					<ul class="alt">
						<li>Student Name | Student Email: user@gmail.com</li>
						<li>Student Name | Student Email: user@gmail.com</li>
					</ul>
				</section>
				<section>
					<h2>Assignments</h2>
					<div class="table-wrapper">
						<table>
							<thead>
								<tr>
									<th>Name</th>
									<th>Description</th>
									<th>Published</th>
									<th>Number Completed</th>
								</tr>
							</thead>
							<tbody>
								<tr>
									<td>Assignment Name</td>
									<td>Description of Assignment</td>
									<td>Yes</td>
									<td>0</td>
								</tr>
								<tr>
									<td>Assignment Name</td>
									<td>Description of Assignment</td>
									<td>Yes</td>
									<td>0</td>
								</tr>
								<tr>
									<td>Assignment Name</td>
									<td>Description of Assignment</td>
									<td>Yes</td>
									<td>0</td>
								</tr>
							</tbody>
						</table>
					</div>
					<a href="#" class="button special">Create New Assignment</a>
				</section>
			</div>
		</section>	
	</div>
	<script src="js/jquery.min.js"></script>
	<script src="js/jquery.scrollex.min.js"></script>
	<script src="js/jquery.scrolly.min.js"></script>
	<script src="js/skel.min.js"></script>
	<script src="js/util.js"></script>
	<!--[if lte IE 8]><script src="js/ie/respond.min.js"></script><![endif]-->
	<script src="js/main.js"></script>
	<script>
		$(document).ready(function() {
			$('#updateName').submit(function(e) {
				e.preventDefault();
				var newName = $('#student-name').val();
				$.ajax({
					url: window.location.href,
					data: newName,
					success: function(data) {
						$('.js-name').text(data);
						console.log(data);
					}
				});
			});
		});
	</script>
</body>
</html>