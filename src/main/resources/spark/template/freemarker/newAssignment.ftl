<!DOCTYPE html>
<html>
<head>
	<title>Create New Assignment</title>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<!--[if lte IE 8]><script src="../../.js/ie/html5shiv.js"></script><![endif]-->
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
				<li><a href="/class/${class.classID}">Class</a></li>
				<li><a href="/class/${class.classID}/new" class="active">New Assignment</a></li>
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
							<input type="text" name="assignment_name" id="assignment_name" value="" placeholder="Assignment Name" />
						</div>
						<div class="12u$">
							<textarea name="assignment_description" id="assignment_description" placeholder="Enter assignment description" rows="6"></textarea>
						</div>
					</div>
				</form>
				<hr />
				<h3>Create Class</h3>
				<div class="row uniform">
					<div class="6u 12u$">
						<input type="text" name="class_name" id="class_name" value="" placeholder="Main Class Name" />
					</div>
					<div class="6u 12u$">
						<ul>
							<li><input type="submit" name="submit" class="special createClass">Create</li>
						</ul>
					</div>
				</div>
				<ul class="actions fit classes"></ul>
			</div>
			<div>
				<div id="editor" class="hidden">//Enter starting code here</div>
			</div>
			<div style="height: 600px;"></div>
			<div class="inner">
				<div class="row uniform">
					<div class="6u 12u$(medium)">
						<li><a href="#" class="button special fit" id="create">Create</a></li>
					</div>
					<div class="6u 12u$(medium)">
						<li><a href="#" class="button special fit" id="createPublish">Create and Publish</a></li>
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
		var currentID = "";
		var codeData = {};
		$(document).ready(function() {
			$(".createClass").click(function(e) {
				e.preventDefault();
				var className = encodeURIComponent($("#class_name").val());
				var mainClass = false;
				if($(".classes li").length == 0)
					mainClass = true;
				$(".classes").append('<li class="button fit special showClass" id="' + className + '">Show ' + $("#class_name").val() + '</li>');
				var newCode = generateCode(mainClass , className);
				codeData[className] = newCode;
				if(mainClass)
					codeData["mainClass"] = className;
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				currentID = className;
				editor.setValue(decodeURIComponent(newCode));
				$("#class_name").attr("placeholder" , "Add Class");
				$("#editor").removeClass("hidden");
			});
			$("#newAssignment").submit(function() {
				createAssignment(false);
			});
			$("#create").click(function() {
				createAssignment(false);
			});
			$("#createPublish").click(function() {
				createAssignment(true);
			});
			$(".showClass").click(function(e) {
				e.preventDefault();
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				editor.setValue(decodeURIComponent(codeData[$(this).attr('id')]));
				currentID = $(this).attr('id');
			});
		});
		function createAssignment(isPublish)
		{
			codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
			var stringData = '{"name" : "' + $("#assignment_name").val() + '" , "description" : "' + $("#assignment_description").val() + '" , "publish" : "' + isPublish + '" , "code" : "' + JSON.stringify(codeData).replace(/"/g , '\"') + '"}';
			$.ajax({
				url: "/class/${class.classID}/new",
				method: 'POST' ,
				dataType: 'text json' ,
				data: stringData ,
				success: function(data) {
					if(data.id != 'no_change')
					{
						window.location.href = "/class/${class.classID}/" + data.id;
					}
					else
						return;
				}
			});
		}
		function generateCode(isMain , className) {
			if(isMain)
				return "public%20class%20" + className + "%20%7B%0A%20%20%20%20public%20static%20void%20main(String%5B%5D%20args)%20%7B%0A%20%20%20%20%20%20%20%20%2F%2FEnter%20starting%20code%20here%0A%20%20%20%20%7D%0A%7D";
			else
				return "public%20class%20" + className  + "%20%7B%0A%20%20%20%20%2F%2FEnter%20starting%20code%20here%0A%7D";
		}
	</script>
</body>
</html>