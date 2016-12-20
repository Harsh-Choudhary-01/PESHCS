<!DOCTYPE HTML>
<!--
	Hyperspace by HTML5 UP
	html5up.net | @ajlkn
	Free for personal and commercial use under the CCA 3.0 license (html5up.net/license)
-->
<html>
<#assign verified = loggedIn>
<#if loggedIn>
	<#assign role = (metadata.app_metadata.role)!"no_role">
</#if>
	<head>
		<title>PESH CS</title>
		<meta charset="utf-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<!--[if lte IE 8]><script src="js/ie/html5shiv.js"></script><![endif]-->
		<link rel="stylesheet" href="css/main.css" />
		<!--[if lte IE 9]><link rel="stylesheet" href="css/ie9.css" /><![endif]-->
		<!--[if lte IE 8]><link rel="stylesheet" href="css/ie8.css" /><![endif]-->
	<body>

		<!-- Sidebar -->
			<section id="sidebar">
				<div class="inner">
					<nav>
						<ul>
							<#if verified>
								<li><a href="#intro">Welcome ${user.nickname}</a></li>
								<#if role != "no_role">
									<#if role == "student">
										<li><a href="#one">Assignments</a></li>
									<#else>
										<li><a href="#one">Classes</a></li>
									</#if>
								</#if>
								<li><a href="logout">Logout</a></li>
							<#else>
								<li><a href="#intro">Welcome</a></li>
							</#if>
						</ul>
					</nav>
				</div>
			</section>

		<!-- Wrapper -->
			<div id="wrapper">

				<!-- Intro -->
					<section id="intro" class="wrapper style1 fullscreen fade-up">
						<div class="inner">
							<h1>PESH CS</h1>
							<p></p>
							<ul class="actions">
							<#if verified>
								<li><a href="#one" class="button scrolly">Get Started</a></li>
							<#else>
								<li><a href="#" class="signup button scrolly">Get Started</a></li>
							</#if>
							</ul>
						</div>
					</section>

				<!-- One -->
					<#if verified>
						<#if role == "no_role">
							<section id="role" class="wrapper style2 roleChoose">
								<div class="inner" style="text-align: center; margin: auto;">
									<h2>What is your role:</h2>
									<ul class="actions vertical">
										<li><a href="#" class="button special student">Student</a></li>
										<li><a href="#" class="button special teacher">Teacher</a></li>
									</ul>
								</div>
							</section>
						<#else>
							<section id="one" class="wrapper style2 spotlights">
								<#if role == "teacher">
									<#list classes as class>
										<section>
											<#assign s = ("images/pic0" + (class?counter % 2 + 1) + ".jpg")>
											<img src="${s}" alt="" data-position="center center"/>
											<div class="content">
												<div class="inner">
													<h2>${class.name}</h2>
													<p>Number of Assignments: ${class.numAssignments}<br>Number of Joined Students: ${class.numJoined}</p>
													<ul class="actions">
														<li><a href="#" class="button">Edit Class</a></li>
													</ul>
												</div>
											</div>
										</section>
									</#list>
									<section>
										<img src="images/pic02.jpg" alt="" data-position="center center"/>
										<div class="content">
											<div class="inner">
												<#if classes?size == 0>
													<h2>No Classes</h2>
												<#else>
													<h2>Create New Class</h2>
												</#if>
												<p>Create a new class by using the appropiate button below.</p>
												<ul class="actions">
													<li><a href="/newclass" class="button">Create Class</a></li>
												</ul>
											</div>
										</div>
									</section>
								<#else>
									<section>
										<a href="#" class="image"><img src="images/pic02.jpg" alt="" data-position="top center" /></a>
										<div class="content">
											<div class="inner">
												<h2>Feugiat consequat</h2>
												<p>Phasellus convallis elit id ullamcorper pulvinar. Duis aliquam turpis mauris, eu ultricies erat malesuada quis. Aliquam dapibus.</p>
												<ul class="actions">
													<li><a href="#" class="button">Learn more</a></li>
												</ul>
											</div>
										</div>
									</section>
									<section>
										<a href="#" class="image"><img src="images/pic03.jpg" alt="" data-position="25% 25%" /></a>
										<div class="content">
											<div class="inner">
												<h2>Ultricies aliquam</h2>
												<p>Phasellus convallis elit id ullamcorper pulvinar. Duis aliquam turpis mauris, eu ultricies erat malesuada quis. Aliquam dapibus.</p>
												<ul class="actions">
													<li><a href="#" class="button">Learn more</a></li>
												</ul>
											</div>
										</div>
									</section>
								</#if>
							</section>
						</#if>
					</#if>
		<!-- Footer -->
			<footer id="footer" class="wrapper style1-alt">
				<div class="inner">
					<ul class="menu">
						<li>&copy; Untitled. All rights reserved.</li><li>Design: <a href="http://html5up.net">HTML5 UP</a></li>
					</ul>
				</div>
			</footer>

		<!-- Scripts -->
			<script src="js/jquery.min.js"></script>
			<script src="js/jquery.scrollex.min.js"></script>
			<script src="js/jquery.scrolly.min.js"></script>
			<script src="js/skel.min.js"></script>
			<script src="js/util.js"></script>
			<!--[if lte IE 8]><script src="js/ie/respond.min.js"></script><![endif]-->
			<script src="js/main.js"></script>
			<script src="https://cdn.auth0.com/js/lock/10.0/lock.min.js"></script>
		    <script>
				if(${(loggedIn && role == "no_role")?c})
					window.location.href="#role";
		      $(document).ready(function()
		      {
		      	$(".student").click(function(e)
		      	{
		      		e.preventDefault();
		      		window.location.href = "/update?role=student";
		      	});
		      	$(".teacher").click(function(e)
		      	{
		      		e.preventDefault();
		      		window.location.href = "/update?role=teacher";
		      	});
		        if(!${loggedIn?c}) {
		        	var lock = new Auth0Lock('${clientId}', '${clientDomain}', {
		        		loginAfterSignup: false ,
			            auth: {
			            	redirect : false ,
			              params: {
			                scope: 'openid user_id name nickname email'
			              }
			            }
		        	});
		        	$('.signup').click(function(e) {
		            	e.preventDefault();
		            	lock.show();
		        	});
		        	lock.on("authenticated", function(authResult) {
		              localStorage.setItem('id_token', authResult.idToken);
		              window.location.href = "/login?token=" + authResult.idToken;
		        	});

		        	lock.on("authorization_error" , function(error)
		        	{
		        		lock.show({
		        			flashMessage: {
		        				type: 'error' ,
		        				text: error.error_description
		        			}
		        		});
		        	});
		        }
		      });
		    </script>
	</body>
</html>