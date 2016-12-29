import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.URLDecoder;
import com.auth0.jwt.JWTVerifier;

import static spark.Spark.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import spark.Request;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;
import static spark.Spark.get;
import java.security.SecureRandom;
import java.math.BigInteger;
import com.heroku.sdk.jdbc.DatabaseUrl;
import org.eclipse.jetty.websocket.api.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;

import org.json.*;
public class Main {
    private static String clientId = System.getenv("AUTH0_CLIENT_ID");
    private static String clientDomain = System.getenv("AUTH0_DOMAIN");
    private static String managementToken = System.getenv("AUTH0_MANAGE");
    public static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    static Map<String , Session> joinedUsers = new ConcurrentHashMap<>();
    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/spark/template/freemarker");
        webSocket("/socket", WebSocketHandler.class);
        SecureRandom random = new SecureRandom();

        get("/hello", (request, response) -> "Hello World");

        get("/class/:classID" , (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            String classID = request.params(":classID");
            Map<String , Object> classInfo = new HashMap<>();
            Connection connection = null;
            Map<String , Object> user = getUser(request);
            String[] joinedStudents = new String[0];
            boolean validClass = false;
            List<String[]> invitedStudents = new ArrayList<>();
            List<String[]> joinedStudentsList = new ArrayList<>();
            if((Boolean) user.get("loggedIn")) {
                user = (Map<String, Object>) user.get("claims");
                try
                {
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT className, cardinality(assignments) AS assignLength , cardinality(joinedStudents) AS joinedLength , (cardinality(invitedStudents) / 2) - 1 AS invitedLength, invitedStudents , joinedStudents FROM classes WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID + "'");
                    while (rs.next())
                    {
                        validClass = true;
                        classInfo.put("className", rs.getString(1));
                        classInfo.put("assignLength", rs.getInt("assignLength"));
                        joinedStudents = (String[]) rs.getArray(6).getArray();
                        String[][] tempInvitedStudents = (String[][]) rs.getArray(5).getArray();
                        for (int i = 1; i < tempInvitedStudents.length; i++) //Starts from 1 to avoid placeholder null value
                        {
                            boolean studentJoined = false;
                            for (int j = 0; j < joinedStudents.length; j++) {
                                if (joinedStudents[j].equals(tempInvitedStudents[i][1]))
                                    studentJoined = true;
                            }
                            if (!studentJoined)
                                invitedStudents.add(tempInvitedStudents[i]);
                        }
                    }
                    if (validClass)
                    {
                        ArrayList<Object> assignments = new ArrayList<>();
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentID text, studentName text , studentEmail text, progress text[][])");
                        rs = stmt.executeQuery("SELECT studentName , studentEmail FROM students WHERE classID = '" + classID + "'");
                        while (rs.next())
                        {
                            joinedStudentsList.add(new String[]{rs.getString("studentName") , rs.getString("studentEmail")});
                        }
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published text , classID text , ownerID text , assignmentID text)");
                        rs = stmt.executeQuery("SELECT name , description , published , assignmentID FROM assignments WHERE ownerID = '" + user.get("user_id") + "'");
                        while(rs.next())
                        {
                            assignments.add(new String[]{rs.getString(1) , rs.getString(2) , rs.getString(3) , rs.getString(4)});
                        }
                        classInfo.put("assignments" , assignments);
                    }
                    classInfo.put("invitedStudents" , invitedStudents);
                    classInfo.put("invitedLength" , invitedStudents.size());
                    classInfo.put("joinedLength", joinedStudentsList.size());
                    classInfo.put("joinedStudents" , joinedStudentsList);
                    classInfo.put("classID" , classID);
                }
                catch (Exception e)
                {
                    System.out.println("class exception:"  + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            if(classInfo.size() != 8)
                halt(404 , "Page Not Found");
            else
                attributes.put("class" , classInfo);
            return  new ModelAndView(attributes , "class.ftl");
        }, new FreeMarkerEngine());

        get("/assignment/:assignmentID" , (request , response) -> {
            Map<String , Object> attributes = new HashMap<>();
            attributes.put("port" , Integer.valueOf(System.getenv("PORT")));
            String assignmentID = request.params(":assignmentID");
            Map<String , Object> user = getUser(request);
            Connection connection = null;
            boolean result = false;
            if((Boolean) user.get("loggedIn"))
            {
                user = (Map<String , Object>) user.get("claims");
                Map<String , Object> metadata = getDynamicUser((String)user.get("user_id"));
                attributes.put("metadata" , metadata);
                try {
                    if (((Map<String, Object>) metadata.get("app_metadata")).get("role").equals("teacher")) {
                        connection = DatabaseUrl.extract().getConnection();
                        Statement stmt = connection.createStatement();
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published text , classID text , ownerID text , assignmentID text)");
                        ResultSet rs = stmt.executeQuery("SELECT classID, name , description from assignments WHERE ownerID = '" + user.get("user_id") + "' AND assignmentID = '" + assignmentID + "'");
                        result = rs.next();
                        if(!result)
                            halt(404 , "Page Not Found");
                        attributes.put("assignment" , new String[]{rs.getString(2) , rs.getString(3)});
                        String classID = rs.getString(1);
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentID text, studentName text , studentEmail text , progress text[][])");
                        rs = stmt.executeQuery("SELECT studentName , unnest(progress[i:i][4:4]) , studentID FROM students a JOIN LATERAL generate_subscripts(a.progress , 1) i on a.progress[i:i][1] = '{{" + assignmentID + "}}' WHERE classID = '" + classID + "'");
                        result = false;
                        ArrayList<Object> students = new ArrayList<>();
                        while(rs.next())
                        {
                            result = true;
                            students.add(new String[]{rs.getString(1) , rs.getString(2) , rs.getString(3)});
                        }
                        attributes.put("students" , students);
                    }
                    else {
                        connection = DatabaseUrl.extract().getConnection();
                        Statement stmt = connection.createStatement();
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentID text, studentName text , studentEmail text , progress text[][])");
                        ResultSet rs = stmt.executeQuery("SELECT classID from students WHERE userID = '" + user.get("user_id") + "'");
                        result = rs.next();
                        if(!result)
                            halt(404 , "Page Not Found");
                        String classID = rs.getString(1);
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published text , classID text , ownerID text , assignmentID text)");
                        rs = stmt.executeQuery("SELECT name , description , code from assignments WHERE assignmentID = '" + assignmentID + "' AND classID = '" + classID + "'");
                        result = rs.next();
                        attributes.put("assignment" , new String[]{rs.getString(1) , rs.getString(2) , rs.getString(3)});
                        if(!result)
                            halt(404 , "Page Not Found");
                        rs = stmt.executeQuery("SELECT ARRAY(SELECT unnest(progress[i:i][2:3])) FROM students a JOIN LATERAL generate_subscripts(a.progress , 1) i on a.progress[i:i][1] = '{{" + assignmentID + "}}' WHERE userID = '" + user.get("user_id") + "'"); //returns code and output stored
                        if(rs.next())
                            attributes.put("progress" , rs.getArray(1).getArray());
                        attributes.put("classID" , classID);
                    }
                    attributes.put("id" , assignmentID);
                }
                catch (Exception e)
                {
                    System.out.println("Exception at live page: " + e);
                    halt(404 , "Page Not Found");
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            else
            {
                halt(404 , "Page Not Found");
            }
            return new ModelAndView(attributes , "editor.ftl");
        } , new FreeMarkerEngine());

        get("/class/:classID/:assignmentID" , (request , response) -> {
            Map<String , Object> attributes = new HashMap<>();
            String classID = request.params(":classID");
            String assignmentID = request.params(":assignmentID");
            if(assignmentID.equals("new")) {
                Map<String , Object> classInfo = new HashMap<>();
                classInfo.put("classID" , classID);
                attributes.put("class" , classInfo);
                return new ModelAndView(attributes, "newAssignment.ftl");
            }
            else
            {
                Map<String , Object> user = getUser(request);
                if((Boolean) user.get("loggedIn")) {
                    user = (Map<String, Object>) user.get("claims");
                    Connection connection = null;
                    try {
                        connection = DatabaseUrl.extract().getConnection();
                        Statement stmt = connection.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT name , description , code , published, assignmentID FROM assignments WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID + "' AND assignmentID = '" + assignmentID + "'");
                        while (rs.next())
                        {
                            attributes.put("name" , rs.getString(1));
                            attributes.put("description" , rs.getString(2));
                            attributes.put("code" , rs.getString(3));
                            attributes.put("published" , rs.getString(4));
                            attributes.put("id" , rs.getString(5));
                            break;
                        }
                    }
                    catch (Exception e) {
                        System.out.println("Excception: " + e);
                    }
                    finally {
                        if(connection != null) try { connection.close(); } catch(SQLException e) {}
                    }
                    if(attributes.size() == 5)
                        attributes.put("classID" , classID);
                    else
                        halt(404 , "Page not Found");
                }
                return new ModelAndView(attributes , "assignment.ftl");
            }
        } , new FreeMarkerEngine());

        get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            Map<String , Object> user = getUser(request);
            attributes.put("loggedIn" , user.get("loggedIn"));
            if((Boolean) attributes.get("loggedIn")) {
                attributes.put("user", user.get("claims"));
                Map<String , Object> metadata = getDynamicUser((String)((Map<String , Object>)attributes.get("user")).get("user_id"));
                attributes.put("metadata" , metadata);
                if(metadata != null)
                {
                    Connection connection = null;
                    try {
                        if (((Map<String, Object>) metadata.get("app_metadata")).get("role").equals("teacher")) {
                            connection = DatabaseUrl.extract().getConnection();
                            Statement stmt = connection.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT className , classID , cardinality(assignments) AS assignLength , cardinality(joinedStudents) AS joinedLength FROM classes WHERE ownerID = '" + ((Map<String, Object>)attributes.get("user")).get("user_id") + "'");
                            ArrayList<Object> classes = new ArrayList<>();
                            while(rs.next())
                            {
                                Map<String, Object> classObject = new HashMap<>();
                                classObject.put("name" , rs.getString(1)); //className
                                classObject.put("classID" , rs.getString(2)); //classID
                                classObject.put("numAssignments" , rs.getInt("assignLength")); //num Assignments
                                classObject.put("numJoined" , rs.getInt("joinedLength")); //num Joined
                                classes.add(classObject);
                            }
                            attributes.put("classes" , classes);
                        }
                        else if(((Map<String, Object>) metadata.get("app_metadata")).get("role").equals("student"))
                        {
                            connection = DatabaseUrl.extract().getConnection();
                            user = (Map<String, Object>) user.get("claims");
                            Statement stmt = connection.createStatement();
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentID text, studentName text , studentEmail text, progress text[][])");
                            ResultSet rs = stmt.executeQuery("SELECT classID from students WHERE userID = '" + user.get("user_id") + "'");
                            String classID = "";
                            while(rs.next())
                            {
                                classID = rs.getString(1);
                                System.out.println("did rs");
                            }
                            if(classID.equals(""))
                                attributes.put("joinedClass" , false);
                            else {
                                System.out.println("joinedClass is true");
                                attributes.put("joinedClass" , true);
                                ArrayList<Object> assignments = new ArrayList<>();
                                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published text , classID text , ownerID text , assignmentID text)");
                                rs = stmt.executeQuery("SELECT name , description , assignmentID FROM assignments WHERE classID = '" + classID + "' AND published = 'true'");
                                while (rs.next()) {
                                    assignments.add(new String[]{rs.getString(1) , rs.getString(2) , rs.getString(3)});
                                }
                                attributes.put("assignments" , assignments);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        System.out.println("Occur" + e);
                    }
                    finally {
                        if(connection != null) try { connection.close(); } catch(SQLException e) {}
                    }
                }
            }
            attributes.put("t" , request.session().attribute("t"));
            request.session().removeAttribute("t");
            attributes.put("message", "Hello World!");
            attributes.put("clientId", clientId);
            attributes.put("clientDomain", clientDomain);
            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

        before("/login", (request, response) -> {
            if (request.raw().getHeader(X_FORWARDED_PROTO) != null) {
                if (request.raw().getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    response.redirect("https://" + request.raw().getServerName() + (request.raw().getPathInfo() == null ? "" : request.raw().getPathInfo()));
                }
            }
            request.session().attribute("token" , request.queryParams("token"));
            response.redirect("/");
        });

        before("/logout", (request, response) -> {
            if (request.raw().getHeader(X_FORWARDED_PROTO) != null) {
                if (request.raw().getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    response.redirect("https://" + request.raw().getServerName() + (request.raw().getPathInfo() == null ? "" : request.raw().getPathInfo()));
                }
            }
            request.session().attribute("token" , null);
            response.redirect("/");
        });

        before("/update" , (request , response) -> {
            if (request.raw().getHeader(X_FORWARDED_PROTO) != null) {
                if (request.raw().getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    response.redirect("https://" + request.raw().getServerName() + (request.raw().getPathInfo() == null ? "" : request.raw().getPathInfo()));
                }
            }
            Map<String , Object> user = getUser(request);
            if((Boolean) user.get("loggedIn")) {
                user = (Map<String, Object>) user.get("claims");
                String role = request.queryParams("role");
                if(role != null && (role.equals("student") || role.equals("teacher"))) {
                    Unirest.patch("https://" + clientDomain + "/api/v2/users/" + user.get("user_id"))
                            .header("authorization", "Bearer " + managementToken)
                            .header("content-type", "application/json")
                            .body("{\"app_metadata\" : { \"role\" : \"" + role + "\" } }")
                            .asString();
                }
            }
           response.redirect("/");
        });

        before("/newclass" , (request, response) -> { //TODO: check to make sure role is teacher
            if (request.raw().getHeader(X_FORWARDED_PROTO) != null) {
                if (request.raw().getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    response.redirect("https://" + request.raw().getServerName() + (request.raw().getPathInfo() == null ? "" : request.raw().getPathInfo()));
                }
            }
            String classID = new BigInteger(30, random).toString(32);
            Map<String, Object> user = getUser(request);
            if((Boolean) user.get("loggedIn")) {
                Map<String, Object> claims = (Map<String, Object>) user.get("claims");
                Connection connection = null;
                try
                {
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS classes (classID text , className text, ownerID text , assignments text[] DEFAULT '{}', joinedStudents text[] DEFAULT '{}' , invitedStudents text[][] DEFAULT '{{null , null}}')");
                    stmt.executeUpdate("INSERT INTO classes(classID , className, ownerID) VALUES ('" + classID + "' , 'New Class' , '" + claims.get("user_id") + "')");
                }
                catch (Exception e)
                {
                    System.out.println("Exception" + e.toString());
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            request.session().attribute("t" , "one");
            response.redirect("/");
        });

        before((request, response) -> {
            if (request.raw().getHeader(X_FORWARDED_PROTO) != null) {
                if (request.raw().getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    response.redirect("https://" + request.raw().getServerName() + (request.raw().getPathInfo() == null ? "" : request.raw().getPathInfo()));
                }
            }
        });

        post("/class/:classID" , (request, response) -> {
            Connection connection = null;
            String classID = request.params(":classID");
            Map<String, Object> user = getUser(request);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonReq = new HashMap<>();
            String newID = "";
            int updated = 0;
            if((Boolean) user.get("loggedIn")) {
                user = (Map<String , Object>) user.get("claims");
                try
                {
                    System.out.println(request.body());
                    jsonReq = mapper.readValue(request.body() , new TypeReference<Map<String , String>>(){});
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    if(jsonReq.get("updating").equals("class_name"))
                        updated = stmt.executeUpdate("UPDATE classes SET className = '" + jsonReq.get("value") + "' WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID +"'");
                    if(jsonReq.get("updating").equals("new_invite"))
                    {
                        newID = new BigInteger(30, random).toString(32);
                        updated = stmt.executeUpdate("UPDATE classes SET invitedStudents = array_cat(invitedStudents , ARRAY['" + jsonReq.get("value") + "' , '" + newID + "']) WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID +"'");
                    }
                }
                catch (Exception e) {
                    System.out.println("Exception on post: " + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            if(updated == 0)
                return "{\"name\" : \"no_change\"}";
            else {
                if(jsonReq.get("updating").equals("class-name"))
                    return "{\"name\" : \"" + jsonReq.get("value") + "\"}";
                else
                    return "{\"name\" : \"" + jsonReq.get("value") + "\" , \"code\" : \"" + newID + "\"}";
            }
        });

        post("/class/:classID/new" , (request , response) -> {
            String classID = request.params(":classID");
            String assignmentID = new BigInteger(30 , random).toString(32);
            ObjectMapper mapper = new ObjectMapper();
            Map<String , Object> jsonReq = new HashMap<>();
            Connection connection = null;
            int updated = 0;
            Map<String , Object> user = getUser(request);
            if((Boolean) user.get("loggedIn"))
            {
                user = (Map<String , Object>) user.get("claims");
                try {
                    jsonReq = mapper.readValue(request.body() , new TypeReference<Map<String , String>>(){});
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    updated = stmt.executeUpdate("UPDATE classes SET assignments = array_cat(assignments , '{" + assignmentID + "}') WHERE classID = '" + classID + "' AND ownerID = '" + user.get("user_id") + "'");
                    if(updated != 0)
                    {
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published text , classID text , ownerID text , assignmentID text)");
                        updated = stmt.executeUpdate("INSERT INTO assignments (name , description , code , published , classID , ownerID , assignmentID) VALUES('" + jsonReq.get("name") + "' , '" + jsonReq.get("description") + "' , $$" +
                                jsonReq.get("code") + "$$ , '" + jsonReq.get("publish") + "' , '" + classID + "' , '" + user.get("user_id") + "' , '" + assignmentID + "')");
                    }
                }
                catch(Exception e)
                {
                    System.out.println("Exception while creating new assignment: " + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
           if(updated == 0)
               return "{\"id\" : \"no_change\"}";
           else
               return "{\"id\" : \"" + assignmentID + "\"}";
        });

        post("/class/:classID/:assignmentID" , (request , response) -> {
            System.out.println("Doing post to not new");
            ObjectMapper mapper = new ObjectMapper();
            Map<String , Object> jsonReq = new HashMap<>();
            String classID = request.params(":classID");
            String assignmentID = request.params(":assignmentID");
            Map<String , Object> user = getUser(request);
            Connection connection = null;
            int updated = 0;
            if((Boolean) user.get("loggedIn")) {
                try {
                    user = (Map<String , Object>) user.get("claims");
                    jsonReq = mapper.readValue(request.body() , new TypeReference<Map<String , String>>(){});
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    if(jsonReq.get("publish").equals("true"))
                        updated = stmt.executeUpdate("UPDATE assignments SET published = 'true' , code = $$" + jsonReq.get("code") + "$$ WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID + "' AND assignmentID = '" + assignmentID + "'");
                    else
                        updated = stmt.executeUpdate("UPDATE assignments SET code = $$" + jsonReq.get("code") + "$$ WHERE ownerID = '" + user.get("user_id") + "' AND classID = '" + classID + "' AND assignmentID = '" + assignmentID + "'");
                }
                catch (Exception e) {
                    System.out.println("Error saving assignment: " + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            if(updated == 0)
                return "failure";
            else
                return "success";
        });

        post("/" , (request, response) -> {
            Map<String, Object> user = getUser(request);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonReq = new HashMap<>();
            Connection connection = null;
            int updated = 0;
            if((Boolean) user.get("loggedIn"))
            {
                user = (Map<String , Object>) user.get("claims"); //TODO: check if student has already joined any classes and also rmbr to check importance of classes metadata in auth0 and if we need that
                try
                {
                    jsonReq = mapper.readValue(request.body() , new TypeReference<Map<String , String>>(){});
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    if(jsonReq.get("updating").equals("join_class"))
                    {
                        ResultSet rs = stmt.executeQuery("SELECT a.* FROM classes a JOIN LATERAL generate_subscripts(a.invitedStudents , 1) i on a.invitedStudents[i:i] = '{{" + jsonReq.get("student_name") + " , " + jsonReq.get("student_id") + "}}' WHERE classID = '" + jsonReq.get("class_id") + "'");
                        while(rs.next())
                        {
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentID text, studentName text , studentEmail text, progress text[][])");
                            stmt.executeUpdate("INSERT INTO students(userID , classID , studentID , studentName , studentEmail) VALUES('" + user.get("user_id") + "' , '" + jsonReq.get("class_id") + "' , '" + jsonReq.get("student_id") + "' , '" + jsonReq.get("student_name") + "' , '" + user.get("email") + "')");
                            updated = stmt.executeUpdate("UPDATE classes SET joinedStudents = array_cat(joinedStudents , '{" + jsonReq.get("student_id") + "}') WHERE classID = '" + jsonReq.get("class_id") + "'");
                            break;
                        }
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Exception on post index: " + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            if(updated == 0)
                return "{\"status\" : \"fail\"}";
            else
                return "{\"status\" : \"success\"}";
        });

        post("/assignment/:assignmentID" , (request , response) -> {
            String assignmentID = request.params(":assignmentID");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonReq = new HashMap<>();
            Connection connection = null;
            int updated = 0;
            Map<String , Object> user = getUser(request);
            if((Boolean) user.get("loggedIn"))
            {
                user = (Map<String , Object>) user.get("claims");
                try
                {
                    jsonReq = mapper.readValue(request.body() , new TypeReference<Map<String , String>>(){});
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    if(jsonReq.get("type").equals("save"))
                    {
                        ResultSet rs = stmt.executeQuery("SELECT i FROM students a JOIN LATERAL generate_subscripts(a.progress , 1) i on a.progress[i:i][1] = '{{" + assignmentID + "}}' WHERE userID = '" + user.get("user_id") + "'");
                        if(rs.next())
                        {
                            int i = rs.getInt(1);
                            updated = stmt.executeUpdate("UPDATE students SET progress[" + i + ":"  + i + "] = '{{" + assignmentID + " , $$" +  jsonReq.get("code")  + "$$, No Output Please Run , N/A}}' WHERE userID = '" + user.get("user_id") + "'");
                        }
                        else
                            updated = stmt.executeUpdate("UPDATE students SET progress = array_cat(progress , '{{" + assignmentID + " , $$" +  jsonReq.get("code")  + "$$, No Output Please Run , N/A}}') WHERE userID = '" + user.get("user_id") + "'");
                    }
                    else if(jsonReq.get("type").equals("compile"))
                    {
                        return compileCode(jsonReq.get("code") , jsonReq.get("input") , (String)user.get("user_id"));
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Error while posting to assign page: " + e);
                }
                finally {
                    if(connection != null) try { connection.close(); } catch(SQLException e) {}
                }
            }
            if(updated == 0)
                return "failure";
            else
                return "success";
        });
    }

    private static Map<String, Object> getUser(Request request) { //Returns a map with a loggedIn value. If the loggedIn value is true also contains a value with key "claims"
        Map<String, Object> user = new HashMap<>();
        Map<String, Object> userInfo;
        if (request.session().attribute("token") == null) {
            user.put("loggedIn", false);
        } else {
            String token = request.session().attribute("token");
            userInfo = checkToken(token);
            if (userInfo.containsKey("loggedIn"))
                user = userInfo;
            else
                user.put("loggedIn", false);
        }
        return user;
    }

    private static Map<String, Object> checkToken(String token) {
        Map<String, Object> values = new HashMap<>();
        final String secret = System.getenv("AUTH0_CLIENT_SECRET");
        final byte[] decodedSecret = Base64.getUrlDecoder().decode(secret);
        try {
            final JWTVerifier verifier = new JWTVerifier(decodedSecret);
            final Map<String, Object> claims = verifier.verify(token);
            values.put("loggedIn", true);
            values.put("claims", claims);
            return values;
        } catch (Exception e) {
            System.out.println("Invalid token");
            return values;
        }
    }

    private static Map<String, Object> getDynamicUser(String userID) //returns dynamic user info like metadata
    {
        try {
            HttpResponse<String> response = Unirest.get("https://" + clientDomain + "/api/v2/users/" + userID + "?fields=app_metadata")
                    .header("Authorization", "Bearer " + managementToken)
                    .asString();
            String json = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<Map<String , Object>>(){});
        }
        catch (Exception e)
        {
            System.out.println(e);
            System.out.println("Exception getting dynamic user");
            return null;
        }
    }
    public static void disconnectUser(Session user) {
        if(joinedUsers.containsValue(user))
        {
            Optional<String> key = joinedUsers.keySet().stream().filter(k -> joinedUsers.get(k).equals(user)).findFirst();
            if(key.isPresent())
                joinedUsers.remove(key.get());
        }
    }
    public static void receiveMessage(Session user , String message) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String , String> jsonReq = new HashMap<>();
        Connection connection = null;
        try {
            jsonReq = mapper.readValue(message , new TypeReference<Map<String , String>>(){});
            if(jsonReq.get("type").equals("auth")) {
                Map<String , Object> userInfo = checkToken(jsonReq.get("token"));
                if(userInfo.containsKey("loggedIn")) {
                    userInfo = (Map<String, Object>) userInfo.get("claims");
                    String userID = (String)userInfo.get("user_id");
                    String role = (String)((Map<String , Object>)(getDynamicUser(userID).get("app_metadata"))).get("role");
                    if(role.equals("teacher"))
                        joinedUsers.put(userID , user);
                    else
                    {
                        connection = DatabaseUrl.extract().getConnection();
                        Statement stmt = connection.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT studentID from students WHERE userID = '" + userID + "'");
                        if(rs.next())
                            joinedUsers.put(rs.getString(1) , user);
                    }
                }
            }
            else if(jsonReq.get("type").equals("help"))
            {
                Map<String , Object> userInfo = checkToken(jsonReq.get("token"));
                if(userInfo.containsKey("loggedIn"))
                {
                    userInfo = (Map<String , Object>) userInfo.get("claims");
                    String userID = (String)userInfo.get("claims");
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT classID , studentName from students WHERE userID = '" + userID + "'");
                    if(rs.next())
                    {
                        String studentName = rs.getString(2);
                        rs = stmt.executeQuery("SELECT ownerID from classes WHERE classID = '" + rs.getString(1) + "'");
                        if(rs.next())
                        {
                            Session teacher = joinedUsers.get(rs.getString(1));
                            teacher.getRemote().sendString(String.valueOf(new JSONObject()
                            .put("type" , "help")
                            .put("student" , studentName)));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error while receiving message: " + e);
        }
        finally {
            if(connection != null) try { connection.close(); } catch(SQLException e) {}
        }
    }
    private static String compileCode(String encodedCode , String encodedInput , String userID) {
        BufferedReader stdOutput = null;
        BufferedReader runStdOutput = null;
        BufferedWriter runStdIn = null;
        try {
            String code = URLDecoder.decode(encodedCode , "UTF-8");
            File file = new File(userID + "/Main.java");
            file.getParentFile().mkdirs();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(code);
            out.close();
            ProcessBuilder pb = new ProcessBuilder("javac " + userID + "/Main.java");
            pb.redirectErrorStream(true);
            Process compileProcess = pb.start();
            stdOutput = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
            String output = "";
            String temp;
            while((temp = stdOutput.readLine()) != null)
            {
                output += temp;
                output += "\n";
            }
            if(!output.equals(""))
                return output;
            compileProcess.destroy();
            compileProcess.waitFor();
            pb = new ProcessBuilder("java " + userID + "/Main");
            pb.redirectErrorStream(true);
            Map<String , String> env = pb.environment();
            env.clear();
            Process runProcess = pb.start();
            runStdOutput = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            runStdIn = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()));
            runStdIn.write(encodedInput);
            runStdIn.close();
            while((temp = runStdOutput.readLine()) != null) {
                output += temp;
                output += "\n";
            }
            runProcess.destroy();
            runProcess.waitFor();
            return output;
        }
        catch (Exception e) {
            System.out.println("Exception while compiling");
            return "error";
        }
        finally {
            try
            {
                if(stdOutput != null)
                    stdOutput.close();
                if(runStdOutput != null)
                    runStdOutput.close();
                if(runStdIn != null)
                    runStdIn.close();
                FileUtils.deleteDirectory(new File(userID));
            }
            catch (Exception e) {}
        }
    }
}