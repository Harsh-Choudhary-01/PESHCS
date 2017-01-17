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
				<ul class="actions fit classes"></ul>
			</div>
			<div>
				<div id="editor" class="hidden">//Enter starting code here</div>
			</div>
			<div style="height: 600px;"></div>
			<div class="inner">
				<div class="row uniform">
					<#if published == "false">
						<div class="6u 12u$(medium)">
							<a href="#" class="button special fit" id="save">Save</a>
						</div>
						<div class="6u 12u$(medium)">
							<a href="#" class="button special fit" id="publish">Publish and Save</a>
						</div>
					<#else>
						<div class="12u$">
							<a href="#" class="button special fit" id="save">Save</a>
						</div>
					</#if>
					<div class="12u$">
						<a href="/assignment/${id}" class="button special fit">Live View</a>
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
		$(document).ready(function() {
			ace.require("ace/ext/language_tools");
			var editor = ace.edit("editor");
			editor.setTheme("ace/theme/twilight");
			var JavaMode = ace.require("ace/mode/java").Mode;
			var currentID = "";
			editor.session.setMode(new JavaMode());
			editor.setOptions({
			    enableBasicAutocompletion: true,
			    enableSnippets: false,
	        	enableLiveAutocompletion: true
			});
			var codeData = JSON.parse("${code}");
			for (var key in codeData) {
				if(codeData.hasOwnProperty(key)) {
					if(key != "mainClass")
						$(".classes").append('<li class="button fit special showClass" id="' + codeData[key] + '">Show ' + decodeURIComponent(codeData[key]) + '</li>');
				}
			}
			$(".showClass").click(function() {
				e.preventDefault();
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				editor.setValue(decodeURIComponent(codeData[$(this).attr('id')]));
				currentID = $(this).attr('id');
				$("#editor").removeClass("hidden");
			});

			$("#save").click(function() {
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				$.ajax({
					url: window.location.href,
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"code" : "' + JSON.stringify(codeData).replace(/"/g , '\"') + '" , "publish" : "false"}' ,
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
					data: '{"code" : "' + JSON.stringify(codeData).replace(/"/g , '\"') + '" , "publish" : "true"}' ,
					success: function(data) {
						if(data === 'success')
							alert("Published successfully");
						else
							alert("Could not publish please try again");
					}
				});
			});
		});
	</script>
</body>
</html>