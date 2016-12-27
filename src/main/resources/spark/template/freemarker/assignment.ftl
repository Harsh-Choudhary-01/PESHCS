<!DOCTYPE html>
<html>
<head>
	<title>${name}</title>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<!--[if lte IE 8]><script src="../../js/ie/html5shiv.js"></script><![endif]-->
	<link rel="stylesheet" href="../../css/main.css" />
	<style type="text/css" media="screen">
		#editor { 
			position: absolute;
			height: 600px;
			width: 100%;
		}
	</style>
	<!--[if lte IE 9]><link rel="stylesheet" href="../../css/ie9.css" /><![endif]-->
	<!--[if lte IE 8]><link rel="stylesheet" href="../../css/ie8.css" /><![endif]-->
	<script type="text/javascript">
		document.getElementById("editor").innerHTML = decodeURIComponent(${code});
	</script>
</head>
<body>
	<header id="header">
		<a href="/" class="title">PESH CS</a>
		<nav>
			<ul>
				<li><a href="/">Home</a></li>
				<li><a href="/class/${classID}">Class</a></li>
				<li><a href="/class/${classID}/${id}" class="active">${name}</a></li>
			</ul>
		</nav>
	</header>
	<div id="wrapper">
		<section id="main" class="wrapper">
			<div class="inner">
				<h1 class="major">${name}</h1>
				<h3>Description:</h3>
				<p>${description}</p>
				<strong style="color:  red;">Editing and then saving starting code will delete students' progress if published.</strong>
			</div>
			<div>
				<div id="editor">//Enter starting code here</div>
			</div>
			<div style="height: 600px;"></div>
			<div class="inner">
				<div class="row uniform">
					<#if !published>
						<div class="6u 12u$(medium)">
							<li><a href="#" class="button special fit" id="save">Save</a></li>
						</div>
						<div class="6u 12u$(medium)">
							<li><a href="#" class="button special fit" id="publish">Publish and Save</a></li>
						</div>
					<#else>
						<div class="12u$">
							<li><a href="#" class="button special fit" id="save">Save</a></li>
						</div>
					</#if>
					<div class="12u$">
						<li><a href="/assignment/${id}" class="button special fit">Live View</a></li>
					</div>
				</div>
			</div>
		</section>
	</div>
	<script src="../../js/jquery.min.js"></script>
	<script src="../../ace/src-min/ace.js" type="text/javascript" charset="utf-8"></script>
	<script src="../../ace/src-min/mode-java.js" type="text/javascript" charset="utf-8"></script>
	<script src="../../ace/src-min/theme-twilight.js" type="text/javascript" charset="utf-8"></script>
	<script src="../../ace/src-min/ext-language_tools.js" type="text/javascript" charset="utf-8"></script>
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
		$(document).ready(function() {
			$("#save").click(function() {
				$.ajax({
					url: window.location.href,
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"code" : "' + encodeURIComponent(editor.getValue()) + '" , "publish" : "false"}' ,
					success: function(data) {
						if(data === 'success')
							alert("Saved successfully");
						else
							alert("Could not save please try again");
					}
				});
			});
			$("#publish").click(function() {
				$.ajax({
					url: window.location.href,
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"code" : "' + encodeURIComponent(editor.getValue()) + '" , "publish" : "true"}' ,
					success: function(data) {
						if(data === 'success')
							alert("Saved successfully");
						else
							alert("Could not save please try again");
					}
				});
			});
		});
	</script>
</body>
</html>