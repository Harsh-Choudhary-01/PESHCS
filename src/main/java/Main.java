import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

import com.auth0.jwt.JWTVerifier;

import static spark.Spark.*;

import spark.Request;
import spark.template.freemarker.FreeMarkerEngine;
import spark.ModelAndView;

import static spark.Spark.get;

import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {
    static String clientId = System.getenv("AUTH0_CLIENT_ID");
    static String clientDomain = System.getenv("AUTH0_DOMAIN");

    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/spark/template/freemarker");

        get("/hello", (req, res) -> "Hello World");

        get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            Map<String , Object> user = getUser(request);
            attributes.put("loggedIn" , user.get("loggedIn"));
            if((Boolean) attributes.get("loggedIn"))
                attributes.put("user" , user.get("claims"));
            attributes.put("message", "Hello World!");
            attributes.put("clientId", clientId);
            attributes.put("clientDomain", clientDomain);
            return new ModelAndView(attributes, "index.ftl");
        }, new FreeMarkerEngine());

        before("/login", (request, response) -> {
            request.session().attribute("token" , request.queryParams("token"));
            response.redirect("/");
        });

    }

    static Map<String, Object> getUser(Request request) {
        Map<String, Object> user = new HashMap<>();
        Map<String, Object> userInfo;
        if (request.session().attribute("token") == null) {
            user.put("loggedIn", false);
        } else {
            String token = request.session().attribute("token");
            userInfo = checkToken(token);
            if (userInfo.containsKey("loggedIn")) {
                request.session().attribute("token", token);
                user = userInfo;
            } else
                user.put("loggedIn", false);
        }
        return user;
    }

    static Map<String, Object> checkToken(String token) {
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

}