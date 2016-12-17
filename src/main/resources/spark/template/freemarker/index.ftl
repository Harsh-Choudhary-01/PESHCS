<!DOCTYPE HTML>
<!--
	Hyperspace by HTML5 UP
	html5up.net | @ajlkn
	Free for personal and commercial use under the CCA 3.0 license (html5up.net/license)
-->
<html>
<#assign verified = loggedIn>
	<head>
		<title>PESH CS</title>
		<meta charset="utf-8" />
		<meta name="viewport" content="width=device-width, initial-scale=1" />
		<!--[if lte IE 8]><script src="js/ie/html5shiv.js"></script><![endif]-->
		<link rel="stylesheet" href="css/main.css" />
		<!--[if lte IE 9]><link rel="stylesheet" href="css/ie9.css" /><![endif]-->
		<!--[if lte IE 8]><link rel="stylesheet" href="css/ie8.css" /><![endif]-->
	</head>
	<body>

		<!-- Sidebar -->
			<section id="sidebar">
				<div class="inner">
					<nav>
						<ul>
							<#if verified>
								<li><a href="#intro">Welcome ${user.nickname}</a></li>
								<li><a href="#one">Assignments</a></li>
								<li><a href="/logout">Logout</a></li>
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
						<section id="one" class="wrapper style2 spotlights">
							<section>
								<a href="#" class="image"><img src="images/pic01.jpg" alt="" data-position="center center" /></a>
								<div class="content">
									<div class="inner">
										<h2>Sed ipsum dolor</h2>
										<p>Phasellus convallis elit id ullamcorper pulvinar. Duis aliquam turpis mauris, eu ultricies erat malesuada quis. Aliquam dapibus.</p>
										<ul class="actions">
											<li><a href="#" class="button">Learn more</a></li>
										</ul>
									</div>
								</div>
							</section>
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
						</section>
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
		      $(document).ready(function()
		      {
		        if(!${loggedIn?c}) {
		        	var lock = new Auth0Lock('${clientId}', '${clientDomain}', {
		        		loginAfterSignup: false , 
			            auth: {
			            	redirect : false ,
			              params: {
			                scope: 'openid user_id name nickname email picture'
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