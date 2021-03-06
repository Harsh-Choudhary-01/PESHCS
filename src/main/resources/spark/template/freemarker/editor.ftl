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
					<ul class="actions fit classes"></ul>
					<ul class="actions fit">
						<li><a href="#" class="button special fit compile">Run/Save Main</a></li>
						<li><a href="#" class="button special fit compile compileClass">Compile Class</a></li>
					</ul>
					<ul class="actions fit">
						<li><a href="#" class="button special fit reqHelp">Help</a></li>
					</ul>
				</div>
				<div>
					<div id="editor" class="hidden"></div>
				</div>
				<div style="height: 600px;"></div>
				<div class="inner">
					<h2>Enter Input For Program Here</h2>
					<textarea class="stdin"></textarea>
					<hr />
					<section id="output">
						<h2>Output:</h2>
						<pre><code class="outputContainer">${(progress[1])!"No Output Yet"}</code></pre>
					</section>	
				</div>
			<#else>
				<div class="inner">
					<h1 class="major">${assignment[0]}</h1>
					<section>
						<div class="outputArea hidden"><pre><code class="outputContainer"></code></pre><a href="#" class="hideOutput button special">Hide Output</a></div>
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
				<div class="inner editorControls hidden">
					<ul class="actions fit classes"></ul>
				</div>
				<section class="editorControls hidden">
					<div>
						<div id="editor"></div>
					</div>
					<div style="height: 600px;"></div>
				</section>
				<div class="inner editorControls hidden">
					<ul class="actions fit">
						<li><a href="#" class="fit button special editCode">Edit Code</a></li>
						<li><a href="#" class="fit button special compile">Run/Save</a></li>
						<li><a href="#" class="fit button special exitEdit">Exit Editor</a></li>
					</ul>
					<h2>Enter Input For Program Here</h2>
					<textarea class="stdin"></textarea>
					<hr />
					<section id="output">
						<h2>Output:</h2>
						<pre><code class="outputContainer"></code></pre>
					</section>
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
		var webSocket;
		var editor;
		var editing = false;
		var codeData;
		var currentID = "";
		$(document).ready(function() {
			ace.require("ace/ext/language_tools");
			editor = ace.edit("editor");
			editor.setTheme("ace/theme/twilight");
			var JavaMode = ace.require("ace/mode/java").Mode;
			editor.session.setMode(new JavaMode());
			editor.setOptions({
			    enableBasicAutocompletion: true,
			    enableSnippets: false,
	        	enableLiveAutocompletion: true
			});
			var currentStudent;
			webSocket = new WebSocket("wss://peshcsharden.herokuapp.com/socket");
			webSocket.onmessage = function(msg) {handleMessage(msg.data);};
			webSocket.onopen = function(event) {webSocket.send('{"type" : "auth" , "token" : "' + localStorage.getItem("id_token") + '" , "assignID" : "${id}"}')};
			window.setInterval(function() {
				webSocket.send("ping");
			} , 20000);
			if('${role}' === 'student')
			{
				var codeString = "${((progress[0])!assignment[2])!""}";
				if(codeString != "")
				{
					codeData = JSON.parse(codeString.replace(/q\$/g, '"'));
					for (var key in codeData) {
						if(codeData.hasOwnProperty(key)) {
							if(key != "mainClass")
								$(".classes").append('<li class="button fit special showClass" id="' + key + '">Show ' + decodeURIComponent(key) + '</li>');
						}
					}
				}
			}
			if('${role}' === 'teacher')
				editor.setReadOnly(true);
			$(".showClass").click(function(e) {
				e.preventDefault();
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				editor.setValue(decodeURIComponent(codeData[$(this).attr('id')]));
				currentID = $(this).attr('id');
				$("#editor").removeClass("hidden");
			});
			$(".compile").click(function(e) {
				e.preventDefault();
				var compileClass = null;
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				if($(this).hasClass("compileClass") && currentID != "")
					compileClass = currentID;
				$.ajax({
					url: "/assignment/${id}",
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"code" : "' + JSON.stringify(codeData).replace(/"/g , 'q$') + '" , "type" : "compile" , "input" : "' + encodeURIComponent($('.stdin').val()).replace(/'/g, "%27") + '" , "id" : "' +  currentStudent + '" , "editing" : "' + editing + '" , "compileClass" : "' + compileClass + '"}' ,
					success: function(data) {
						window.location.hash = '';
						window.location.hash = "output";
						$('.outputContainer').text(data);
					}
				});
			});
			$(".output").click(function(e) {
				e.preventDefault();
				var id = $(this).attr('id');
				$.ajax({
					url: "/assignment/${id}",
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"type" : "output" , "id" : "' + id + '"}' ,
					success: function(data) {
						$('.outputArea').removeClass('hidden');
						$('.outputContainer').text(data);
					}
				});
			});
			$(".code").click(function(e) {
				e.preventDefault();
				var id = $(this).attr('id');
				$.ajax({
					url: "/assignment/${id}",
					method: 'POST' ,
					dataType: 'text' ,
					data: '{"type" : "code" , "id" : "' + id + '"}' ,
					success: function(data) {
						if(data === 'Student has no saved code yet')
						{
							alert('Student has no saved code yet');
						}
						else if(data != 'failure') {
							$('.outputContainer').text('');
							if(editing)
							{
								webSocket.send('{"type" : "exitEdit" , "id" : "' + currentStudent + '" , "token" : "' +  localStorage.getItem("id_token")  + '" , "code" : "' + JSON.stringify(codeData).replace(/"/g , 'q$') + '" , "assignID" : "${id}"}');
							}
							editing = false;
							editor.setReadOnly(true);
							codeData = JSON.parse(data.replace(/q\$/g, '"'));
							var mainCode = "";
							$(".classes li").remove();
							currentID = codeData.mainClass;
							mainCode = codeData[currentID];
							for (var key in codeData) {
								if(codeData.hasOwnProperty(key)) {
									if(key != "mainClass") {
										$(".classes").append('<li class="button fit special showClass" id="' + key + '">Show ' + decodeURIComponent(key) + '</li>');
									}
								}
							}
							$(".showClass").click(function(e) {
								console.log("Clicked show class");
								e.preventDefault();
								if(currentID != "")
								{
									console.log("Current ID before: " + currentID);
									codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
								}
								editor.setValue(decodeURIComponent(codeData[$(this).attr('id')]));
								currentID = $(this).attr('id');
								console.log("Stuff setting editor to: " + decodeURIComponent(codeData[$(this).attr('id')]));
								console.log("Current ID after: " + currentID);
							});
							$('.editorControls').removeClass('hidden');
							editor.setValue(decodeURIComponent(mainCode));
							currentStudent = id;
						}
						else {
							alert("Could not retrieve code please try again");
						}
					}
				});
			});
			$(".exitEdit").click(function(e) {
				e.preventDefault();
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				$('.outputContainer').text('');
				if(editing)
				{
					webSocket.send('{"type" : "exitEdit" , "id" : "' + currentStudent + '" , "token" : "' +  localStorage.getItem("id_token")  + '" , "code" : "' + JSON.stringify(codeData).replace(/"/g , 'q$') + '" , "assignID" : "${id}"}');
				}
				editing = false;
				editor.setReadOnly(true);
				$('.editorControls').addClass('hidden');
			});
			$(".editCode").click(function(e) {
				e.preventDefault();
				if(!editing)
				{
					webSocket.send('{"type" : "edit" , "token" : "' + localStorage.getItem("id_token") + '" , "id" : "' + currentStudent + '" , "assignID" : "${id}"}');
					alert("Downloading newest version of student code");
				}
			});
			$('.hideOutput').click(function(e) {
				e.preventDefault();
				$('.outputArea').addClass('hidden');
				$('.outputContainer').text('');
			});
			$('.reqHelp').click(function(e) {
				e.preventDefault();
				webSocket.send('{"type" : "help" , "token" : "' + localStorage.getItem("id_token") + '" , "assignID" : "${id}"}');
				alert("Request sent");
			});
		});
		function handleMessage(msg) {
			console.log("Handling message: " + message);
			var message = JSON.parse(msg);
			if(message.type === 'help' && '${role}' === 'teacher') //called on teacher side when student requests help
				alert(message.student + " is asking for help for assignment: " + message.assignment);
			else if(message.type === 'requestEdit') //called on student side when teacher requests to edit
			{
				if(currentID != "")
					codeData[currentID] = encodeURIComponent(editor.getValue()).replace(/'/g, "%27");
				var codeString = JSON.stringify(codeData).replace(/"/g , 'q$');
				var codeMessage = {
					"code" :  codeString ,
					"type" : "sendCode" ,
					"token" : localStorage.getItem("id_token") ,
					"assignID" : "${id}"
				};
				webSocket.send(JSON.stringify(codeMessage));
				editor.setReadOnly(true);
				alert("Teacher is locking code to edit");
			}
			else if(message.type === 'editGranted') { //called on teacher side once student has sent latest version of code
				editing = true;
				editor.setReadOnly(false);
				codeData = JSON.parse(message.code.replace(/q\$/g, '"'));
				if(currentID != "")
					editor.setValue(decodeURIComponent(codeData[currentID]));
				else
					editor.setValue("//Please click one of the show class buttons");
			}
			else if(message.type === 'exitEdit') { //called on student side once teacher exits editor for student
				editor.setReadOnly(false);
				codeData = JSON.parse(message.code.replace(/q\$/g, '"'));
				if(currentID != "")
					editor.setValue(decodeURIComponent(codeData[currentID]));
				else
					editor.setValue("//Please click one of the show class buttons");
				alert("Teacher has finished editing code");
			}
		}
	</script>
</body>
</html>