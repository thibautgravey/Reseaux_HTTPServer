package http.server;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * <p>
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 *
 * @author Jeff Heaton
 * @version 1.0
 */
public enum StatusCode {

    CODE_200("200 OK"),

    CODE_204("204 No Content"),
    CODE_302("302 Found"),
    CODE_404("404 Not Found"),
    CODE_201("Created"),
    CODE_500("500 Internal Server Error"),
    CODE_503("503 Service Unavailable");

    private final String text;

    StatusCode(String text){
        this.text = text;
    }

    @Override
    public String toString(){
        return this.text;
    }
}
