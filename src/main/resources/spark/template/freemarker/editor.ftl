<!DOCTYPE html>
<html>
<#assign role = (metadata.app_metadata.role)!"no_role">
<head>
	<title>${assignment[0]}</title>
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<!--[if lte IE 8]><script src="../js/ie/html5shiv.js"></script><![endif]-->
	<link rel="stylesheet" href="../css/main.css" />
	<style type="text/css" media="screen">
	    #editor { 
	        position: absolute;
	        height: 600px;
	        width: 100%;
	    }
	</style>
	<#if role == "student">
		<script type="text/javascript">
			document.getElementById("editor").innerHTML = decodeURIComponent('${(progress[0])!assignment[2]}');
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
	</#if>
	<!--[if lte IE 9]><link rel="stylesheet" href="../css/ie9.css" /><![endif]-->
	<!--[if lte IE 8]><link rel="stylesheet" href="../css/ie8.css" /><![endif]-->
</head>
<body>
	<header id="header">
		<a href="/" class="title">PESH CS</a>
		<nav>
			<ul>
				<li><a href="/">Home</a></li>
				<li><a href="/assignment/${id}" class="active">${assignment[0]}</a></li>
			</ul>
		</nav>
	</header>
	<div id="wrapper">
		<section id="main" class="wrapper">
			<#if role == "student">
				<div class="inner">
					<h1 class="major">${assignment[0]}</h1>
					<ul class="actions fit">
						<li><a href="#" class="button special fit">Compile</a></li>
						<li><a href="#" class="button special fit save">Save</a></li>
						<li><a href="#" class="button special fit">Help</a></li>
					</ul>
				</div>
				<div>
					<div id="editor"></div>
				</div>
				<div style="height: 600px;"></div>
				<div class="inner">
					<h2>Output:</h2>
					<hr />
					<pre>${(progress[1])!"No Output Yet"}</pre>	
				</div>
			<#else>
				<div class="inner">
					<h1 class="major">${assignment[0]}</h1>
					<section>
						<div class="outputArea hidden"></div>
						<h2>Students</h2>
						<div class="table-wrapper">
							<#if students?has_content>
								<table>
									<thead>
										<tr>
											<th>Student Name</th>
											<th>Compiling</th>
											<th>Output</th>
											<th>Code</th>
										</tr>
									</thead>
									<tbody>
										<#list students as student>
											<tr>
												<td>${student[0]}</td>
												<td>${student[1]}</td>
												<td><a href="#" id="${student[2]}" class="output button special">Show</a></td>
												<td><a href="#" id="${student[2]}" class="button special code">Show</a></td>
											</tr>
										</#list>
									</tbody>
								</table>
							</#if>
						</div>
					</section>
				</div>
				<div>
					<div id="editor" class="hidden"></div>
				</div>
				<div style="height: 600px;" class="hidden overlapper"></div>
				<div class="inner hidden editButton">
					<div>
						<ul class="actions fit">
							<li><a href="#" class="fit button special editCode">Edit Student Code</a></li>
						</ul>
					</div>
				</div>
			</#if>
		</section>
	</div>
	<script src="../js/jquery.min.js"></script>
	<script src="../ace/src-min/ace.js" type="text/javascript" charset="utf-8"></script>
	<script src="../ace/src-min/mode-java.js" type="text/javascript" charset="utf-8"></script>
	<script src="../ace/src-min/theme-twilight.js" type="text/javascript" charset="utf-8"></script>
	<script src="../ace/src-min/ext-language_tools.js" type="text/javascript" charset="utf-8"></script>
	<script type="text/javascript">
		$(document).ready(function() {
			$(".save").click(function(e) {
				e.preventDefault();
				$.ajax({
					url: "/assignment/${id}",
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"code" : "' + encodeURIComponent(editor.getValue()) + '" , "type" : "save"}' ,
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