package http.server;

/**
 * Represent the return statusCode from HTTP Request
 *
 * @author Branchereau Corentin
 * @author Gravey Thibaut
 * @version 1.0
 */
public enum StatusCode {

    CODE_200("200 OK"),
    CODE_201("201 Created"),
    CODE_401("401 Unauthorized"),
    CODE_403("403 Forbidden"),
    CODE_404("404 Not Found"),
    CODE_500("500 Internal Server Error"),
    CODE_501("501 Not Implemented"),
    CODE_505("505 HTTP Version Not Supported");

    private final String text;

    /**
     * String representation of a StatusCode for HTTP response
     * @param text
     */
    StatusCode(String text){
        this.text = text;
    }

    /**
     * support string representation
     * @return textual representation for HTTP response
     */
    @Override
    public String toString(){
        return this.text;
    }
}
