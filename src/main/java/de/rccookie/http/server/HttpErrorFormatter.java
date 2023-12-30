package de.rccookie.http.server;

import de.rccookie.http.ContentType;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.json.IllegalJsonTypeException;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Console;
import de.rccookie.xml.Node;
import de.rccookie.xml.Text;
import org.jetbrains.annotations.NotNull;

/**
 * An error formatter generates an appropriate response message for a given
 * {@link HttpRequestFailure}.
 */
public interface HttpErrorFormatter {

    /**
     * A default formatter which returns a json or xml error information.
     */
    HttpErrorFormatter DEFAULT = (response, failure) -> {
        if(failure.code().type() == ResponseCode.Type.SERVER_ERROR)
            Console.error(failure);

        double useJson, useXml;
        //noinspection ConstantValue
        if(response.request() == null) { // The default formatter may be used as fallback formatter if the request could not be parsed
            useJson = 1;
            useXml = 0;
        } else try {
            useJson = response.request().accept().getWeight(ContentType.JSON);
            useXml = response.request().accept().getWeight(ContentType.XML);
        } catch (IllegalArgumentException e) {
            // Error parsing mime types
            useJson = 1;
            useXml = 0;
        }

        if(useJson >= useXml) {
            JsonObject json = new JsonObject(
                    "error", true,
                    "code", response.code().code(),
                    "name", response.code().name()
            );
            if(failure.message() != null)
                json.put("message", failure.message());
            if(failure.detail() != null) try {
                json.put("detail", failure.detail());
            } catch (IllegalJsonTypeException e) {
                json.put("detail", failure.toString());
            }
            response.setJson(json);
        } else {
            Node xml = new Node(
                    "response",
                    new Node("error", new Text("true")),
                    new Node("code", new Text(response.code().code() + "")),
                    new Node("name", new Text(response.code().name()))
            );
            if(failure.message() != null)
                xml.children.add(new Node("message", new Text(failure.message())));
            if(failure.detail() != null)
                xml.children.add(new Node("detail", new Text(failure.toString())));
            response.setXML(xml);
        }
    };

    /**
     * Sets the response to the default error message format, but <b>does not</b>
     * send the response.
     *
     * @param response The response to be formatted to an error response
     * @param failure The error details to be included in the response
     */
    void format(HttpResponse.Sendable response, @NotNull HttpRequestFailure failure);
}
