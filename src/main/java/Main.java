import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import com.auth0.jwt.JWTVerifier;

import static spark.Spark.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
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
                            System.out.println("classes is: " + classes);
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
                HttpResponse<JsonNode> appMetadata = Unirest.get("https://" + clientDomain + "/api/v2/users/" + claims.get("user_id") + "?fields=app_metadata")
                        .header("authorization", "Bearer " + managementToken)
                        .asJson();
                String classArray;
                if(appMetadata.getBody().getObject().getJSONObject("app_metadata").has("classes"))
                    classArray = appMetadata.getBody().getObject().getJSONObject("app_metadata").getJSONArray("classes").put(classID).toString();
                else
                    classArray = "[\"" + classID + "\"]";
                HttpResponse<String> res = Unirest.patch("https://" + clientDomain + "/api/v2/users/" + claims.get("user_id"))
                        .header("authorization", "Bearer " + managementToken)
                        .header("content-type", "application/json")
                        .body("{\"app_metadata\" : { \"classes\" : " + classArray + "} }")
                        .asString();
                Connection connection = null;
                try
                {
                    connection = DatabaseUrl.extract().getConnection();
                    Statement stmt = connection.createStatement();
                    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS classes (classID text , className text, ownerID text , assignments text[] DEFAULT '{}', joinedStudents text[] DEFAULT '{}' , invitedStudents text[] DEFAULT '{}')");
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