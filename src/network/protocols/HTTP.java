package network.protocols;

import java.io.Serializable;

import network.KnownPort;

/**
 * A stripped-down version of the HTTP protocol. There are no headers; the only
 * methods are GET, PUT, POST, and DELETE; and only the most basic responses
 * are included.
 *
 * @author Luke Maurer
 */
public final class HTTP {
    public static final KnownPort PORT = KnownPort.HTTP;
    
    public static class Request implements Serializable {
        public static enum Method {
            GET,
            PUT,
            POST,
            DELETE,
        }
        
        public final Method method;
        public final String resource;
        
        public Request(Method method, String resource) {
            if (method == null || resource == null)
                throw new NullPointerException();
            
            this.method = method;
            this.resource = resource;
        }
        
        @Override
        public String toString() {
            return "HTTP " + method + ' ' + resource;
        }
        
        private static final long serialVersionUID = 1L;
    }
    
    public static class Response implements Serializable {
        public static enum Status {
            OK,
            REDIRECT,
            BAD_REQUEST,
            NOT_FOUND,
            TEAPOT,
            SERVER_ERROR,
            NOT_IMPLEMENTED,
        }
        
        public final Status status;
        public final String content;
        
        public Response(Status status, String content) {
            if (status == null)
                throw new NullPointerException();
            
            this.status = status;
            this.content = content;
        }

        @Override
        public String toString() {
            switch (status) {
                case OK:
                    return "200 OK";
                case REDIRECT:
                    return "302 Redirect [" + content + "]";
                case BAD_REQUEST:
                    return "400 Bad Request";
                case NOT_FOUND:
                    return "404 Not Found";
                case TEAPOT:
                    return "418 I'm a teapot";
                case SERVER_ERROR:
                    return "500 Internal Server Error";
                case NOT_IMPLEMENTED:
                    return "501 Not Implemented";
                default:
                    return status.toString();
            }
        }
        
        private static final long serialVersionUID = 1L;
    }
    
    private HTTP() { }
}
