<!DOCTYPE html>
<html>
<head>
	<title>Editor</title>
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<!--[if lte IE 8]><script src="../js/ie/html5shiv.js"></script><![endif]-->
	<link rel="stylesheet" href="css/main.css" />
	<style type="text/css" media="screen">
	    #editor { 
	        position: absolute;
	        height: 75%;
	        width: 100%;
	    }
	</style>
	<!--[if lte IE 9]><link rel="stylesheet" href="../css/ie9.css" /><![endif]-->
	<!--[if lte IE 8]><link rel="stylesheet" href="../css/ie8.css" /><![endif]-->
</head>
<body>
	<header id="header">
		<a href="/" class="title">PESH CS</a>
		<nav>
			<ul>
				<li><a href="/">Home</a></li>
				<li><a href="/class" class="active">Assignment</a></li>
			</ul>
		</nav>
	</header>
	<div id="wrapper">
		<section id="main" class="wrapper">
			<div class="inner">
				<h1 class="clicker">Test</h1>
			</div>
			<div>
				<div id="editor">System.out.println("Harsh is the best");</div>
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
		$(document).ready(function()
		{
			$('.clicker').click(function(e)
			{
				console.log("This is the stuff: " + editor.getValue());
			})
		});
	</script>
</body>
</html>