package network.software;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import network.AbstractProcess;
import network.Message;
import network.OperatingSystem.DisconnectedException;
import network.protocols.HTTP;

/**
 * A process implementing a simple HTTP server. The HTTP here is that defined
 * by the {@link HTTP} class. 
 *
 * @author Luke Maurer
 */
public class HTTPServer extends AbstractProcess {
    private final ConcurrentNavigableMap<String, Handler> namespace =
        new ConcurrentSkipListMap<String, Handler>(
                String.CASE_INSENSITIVE_ORDER);
    
    /**
     * Add content to the server. Subsequent requests for the given location
     * will return the given content.
     * 
     * @param location The location at which to add the content.
     * @param content The content to add.
     */
    public void addFile(String location, String content) {
        addHandler(location, new SimpleHandler(false, content));
    }
    
    /**
     * Add a redirect to the server. Subsequent requests for the given location
     * will return a redirect to the given URL.
     * 
     * @param location The location at which to add the redirect.
     * @param targetUrl The URL to redirect to.
     */
    public void addRedirect(String location, String targetUrl) {
        addHandler(location, new SimpleHandler(true, targetUrl));
    }
    
    /**
     * Install a request handler to serve requests dynamically.
     * 
     * @param location The location at which to install the handler.
     * @param handler The handler to install.
     * 
     * @see Handler
     */
    public void addHandler(String location, Handler handler) {
        namespace.put(location, handler);
    }
    
    protected void run() throws InterruptedException {
        try {
            while (true) {
                final Message<?> message = os().receive(HTTP.PORT);
                if (!message.hasType(HTTP.Request.class)) {
                    sendResponse(message,
                            HTTP.Response.Status.BAD_REQUEST, null);
                    continue;
                }
                
                final HTTP.Request request = message.dataAs(HTTP.Request.class);
                switch (request.method) {
                    case GET:
                        final Handler handler = namespace.get(
                                request.resource.replaceAll("\\?*", ""));
                        if (handler == null)
                            sendResponse(message,
                                    HTTP.Response.Status.NOT_FOUND, null);
                        else
                            sendResponse(message,
                                    handler.handleRequest(request));
                            
                        break;
                    default:
                        sendResponse(message,
                                HTTP.Response.Status.NOT_IMPLEMENTED, null);
                }
            }
        } catch (DisconnectedException e) {
            os().logger().warning("Disconnected; shutting down");
        }
    }
    
    private void sendResponse(Message<?> requestMessage, HTTP.Response response)
            throws DisconnectedException, InterruptedException {
        os().send(requestMessage.source, HTTP.PORT.number(),
                requestMessage.sourcePort, response);
    }
    
    private void sendResponse(
            Message<?> requestMessage, HTTP.Response.Status status, String content)
                throws DisconnectedException, InterruptedException {
        sendResponse(requestMessage, new HTTP.Response(status, content));
    }
    
    /**
     * API for dynamic handling of HTTP requests. 
     *
     * @see HTTPServer#addHandler(String, Handler)
     *
     * @author Luke Maurer
     */
    public static interface Handler {
        /**
         * Process an HTTP request, returning the response to send to the
         * client.
         * 
         * @param request The request to process.
         * @return The response.
         */
        HTTP.Response handleRequest(HTTP.Request request);
    }
    
    private final class SimpleHandler implements Handler {
        private final boolean redirect;
        private final String content;
        
        private SimpleHandler(boolean redirect, String content) {
            this.redirect = redirect;
            this.content = content;
        }
        
        public HTTP.Response handleRequest(HTTP.Request request) {
            return new HTTP.Response(
                    redirect ? HTTP.Response.Status.REDIRECT :
                        HTTP.Response.Status.OK, content);
        }
    }
}
