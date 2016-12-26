import java.sql.*;
import java.util.*;

import com.auth0.jwt.JWTVerifier;

import static spark.Spark.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import freemarker.ext.beans.HashAdapter;
import spark.Request;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;
import static spark.Spark.get;
import java.security.SecureRandom;
import java.math.BigInteger;
import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {
    private static String clientId = System.getenv("AUTH0_CLIENT_ID");
    private static String clientDomain = System.getenv("AUTH0_DOMAIN");
    private static String managementToken = System.getenv("AUTH0_MANAGE");
    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/spark/template/freemarker");
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
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentCode text, studentName text , studentEmail text)");
                        rs = stmt.executeQuery("SELECT studentName , studentEmail FROM students WHERE classID = '" + classID + "'");
                        while (rs.next())
                        {
                            joinedStudentsList.add(new String[]{rs.getString("studentName") , rs.getString("studentEmail")});
                        }
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published boolean , classID text , ownerID text , assignmentID text)");
                        rs = stmt.executeQuery("SELECT name , description , published FROM assignments WHERE ownerID = '" + user.get("user_id") + "'");
                        while(rs.next())
                        {
                            assignments.add(new String[]{rs.getString(1) , rs.getString(2) , rs.getString(3)});
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

        get("/class/:classID/:assignmentID" , (request , response) -> {
            Map<String , Object> attributes = new HashMap<>();
            String classID = request.params(":classID");
            String assignmentID = request.params(":assignmentID");
            if(assignmentID.equals("new"))
                return new ModelAndView(attributes , "newAssignment.ftl");
            else
            {
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
                            Statement stmt = connection.createStatement();
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentCode text, studentName text , studentEmail text)");
                            ResultSet rs = stmt.executeQuery("SELECT classID from students WHERE userID = '" + user.get("user_id") + "'");
                            String classID = "";
                            while(rs.next())
                            {
                                classID = rs.getString(1);
                            }
                            if(classID.equals(""))
                                attributes.put("joinedClass" , false);
                            else {
                                attributes.put("joinedClass" , true);
                                ArrayList<Object> assignments = new ArrayList<>();
                                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published boolean , classID text , ownerID text , assignmentID text)");
                                rs = stmt.executeQuery("SELECT name , description , assignmentID FROM assignments WHERE classID = '" + classID + "' AND published = TRUE");
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
            request.session().attribute("token" , request.queryParams("token"));
            response.redirect("/");
        });

        before("/logout", (request, response) -> {
            request.session().attribute("token" , null);
            response.redirect("/");
        });

        before("/update" , (request , response) -> {
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
            String classID = request.params("classID");
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
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS assignments(name text , description text, code text , published boolean , classID text , ownerID text , assignmentID text)");
                        updated = stmt.executeUpdate("INSERT INTO assignments (name , description , code , published , classID , ownerID , assignmentID) VALUES('" + jsonReq.get("name") + "' , '" + jsonReq.get("description") + "' , '" +
                                jsonReq.get("code") + "' , '" + jsonReq.get("publish") + "' , '" + classID + "' , '" + user.get("user_id") + "' , '" + assignmentID + "')");
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
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS students(userID text , classID text, studentCode text, studentName text , studentEmail text)");
                            stmt.executeUpdate("INSERT INTO students(userID , classID , studentCode , studentName , studentEmail) VALUES('" + user.get("user_id") + "' , '" + jsonReq.get("class_id") + "' , '" + jsonReq.get("student_id") + "' , '" + jsonReq.get("student_name") + "' , '" + user.get("email") + "')");
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


}