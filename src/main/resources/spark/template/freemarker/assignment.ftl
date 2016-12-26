<!DOCTYPE html>
<html>
<head>
	<title>Create New Assignment</title>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<!--[if lte IE 8]><script src="js/ie/html5shiv.js"></script><![endif]-->
	<link rel="stylesheet" href="css/main.css" />
	<style type="text/css" media="screen">
		#editor { 
			position: absolute;
			height: 600px;
			width: 100%;
		}
	</style>
	<!--[if lte IE 9]><link rel="stylesheet" href="css/ie9.css" /><![endif]-->
	<!--[if lte IE 8]><link rel="stylesheet" href="css/ie8.css" /><![endif]-->
</head>
<body>
	<header id="header">
		<a href="/" class="title">PESH CS</a>
		<nav>
			<ul>
				<li><a href="/">Home</a></li>
				<li><a href="/class/classID">Class</a></li>
				<li><a href="/class/classID/new" class="active">New Assignment</a></li>
			</ul>
		</nav>
	</header>
	<div id="wrapper">
		<section id="main" class="wrapper">
			<div class="inner">
				<h1 class="major">Create New Assignment</h1>
				<form method="post" action="#" autocomplete="off" id="newAssignment">
					<div class="row uniform">
						<div class="12u$">
							<input type="text" name="assignment-name" id="assignment_name" value="" placeholder="Assignment Name" />
						</div>
						<div class="12u$">
							<textarea name="assignment-description" id="assignment-description" placeholder="Enter assignment description" rows="6"></textarea>
						</div>
					</div>
				</form>
			</div>
			<div>
				<div id="editor">//Enter starting code here</div>
			</div>
			<div style="height: 600px;"></div>
			<div class="inner">
				<div class="row uniform">
					<div class="6u 12u$(medium)">
						<li><a href="#" class="button special fit">Create</a></li>
					</div>
					<div class="6u 12u$(medium)">
						<li><a href="#" class="button special fit">Create and Publish</a></li>
					</div>
				</div>
			</div>
		</section>
	</div>
	<script src="js/jquery.min.js"></script>
	<script src="ace/src-min/ace.js" type="text/javascript" charset="utf-8"></script>
	<script src="ace/src-min/mode-java.js" type="text/javascript" charset="utf-8"></script>
	<script src="ace/src-min/theme-twilight.js" type="text/javascript" charset="utf-8"></script>
	<script src="ace/src-min/ext-language_tools.js" type="text/javascript" charset="utf-8"></script>
	<script type="text/javascript">
		ace.require("ace/ext/language_tools");
		var editor = ace.edit("editor");
		editor.setTheme("ace/theme/twilight");
		var JavaMode = ace.require("ace/mode/java").Mode;
		editor.session.setMode(new JavaMode());
		editor.setOptions({
		    enableBasicAutocompletion: true,
		    enableSnippets: false,
        	enableLiveAutocompletion: true
		});
	</script>
</body>
</html>