/**
 * Simple classes with data structures defining protocols. For instance, the
 * {@link network.protocols.HTTP} class defines the port
 * ({@link network.protocols.HTTP#PORT}) to use and the
 * request ({@link network.protocols.HTTP.Request}) and response
 * ({@link network.protocols.HTTP.Response}) types,
 * including enums for request methods
 * ({@link network.protocols.HTTP.Request.Method}) and
 * response statuses ({@link network.protocols.HTTP.Response.Status}). These
 * should be used by simulated HTTP servers and clients; for instance, a client
 * should send a server a {@link network.Message}{@code <HTTP.Request>} object
 * on the port denoted by {@code HTTP.PORT}. 
 */
package network.protocols;