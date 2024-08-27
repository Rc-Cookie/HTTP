package de.rccookie.http.server;

import java.util.Map;
import java.util.function.BiConsumer;

import de.rccookie.http.ContentType;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.json.IllegalJsonTypeException;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonStructure;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import de.rccookie.xml.Text;
import de.rccookie.xml.XML;
import org.jetbrains.annotations.NotNull;

final class DefaultErrorFormatter implements HttpErrorFormatter {

    public static final String DEFAULT_CSS;
    static {
        try {
            //noinspection resource,DataFlowIssue
            DEFAULT_CSS = new String(DefaultErrorFormatter.class.getResourceAsStream("/stylesheets/default.css").readAllBytes());
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
    }
    public static final DefaultErrorFormatter INSTANCE = new DefaultErrorFormatter();

    private DefaultErrorFormatter() { }

    private static final Map<ContentType, BiConsumer<HttpResponse.Editable, HttpRequestFailure>> AVAILABLE_FORMATS = Map.of(
            ContentType.JSON, DefaultErrorFormatter::formatJson,
            ContentType.XML, DefaultErrorFormatter::formatXml,
            ContentType.HTML, DefaultErrorFormatter::formatHtml,
            ContentType.XHTML, DefaultErrorFormatter::formatXHtml
    );

    @Override
    public void format(HttpResponse.Editable response, @NotNull HttpRequestFailure failure) {
        if(failure.code().type() == ResponseCode.Type.SERVER_ERROR)
            Console.error(failure);

        //noinspection ConstantValue
        if(response.request() == null) { // The default formatter may be used as fallback formatter if the request could not be parsed
            formatJson(response, failure);
        } else try {
            ContentType preferredType = response.request().accept().getPreferred(AVAILABLE_FORMATS.keySet());
            if(preferredType != ContentType.JSON && response.request().accept().getWeight(preferredType) == response.request().accept().getWeight(ContentType.JSON))
                preferredType = ContentType.JSON;
            AVAILABLE_FORMATS.get(preferredType).accept(response, failure);
        } catch(IllegalArgumentException e) {
            // Error parsing mime types
            formatJson(response, failure);
        }
    }

    private static void formatJson(HttpResponse.Editable response, HttpRequestFailure failure) {
        JsonObject json = new JsonObject(
                "error", true,
                "code", response.code().code(),
                "name", response.code().httpName()
        );
        if(failure.message() != null)
            json.put("message", failure.message());
        if(failure.detail() != null) try {
            json.put("detail", failure.detail());
        } catch(IllegalJsonTypeException e) {
            json.put("detail", failure.detail().toString());
        }
        response.setJson(json);
    }

    private static void formatXml(HttpResponse.Editable response, HttpRequestFailure failure) {
        Node xml = new Node(
                "response",
                new Node("error", new Text("true")),
                new Node("code", new Text(response.code().code() + "")),
                new Node("name", new Text(response.code().httpName()))
        );
        if(failure.message() != null)
            xml.children.add(new Node("message", new Text(failure.message())));
        if(failure.detail() != null)
            xml.children.add(new Node("detail", new Text(failure.detail().toString())));
        response.setXML(xml);
    }

    private static void formatHtml(HttpResponse.Editable response, HttpRequestFailure failure) {
        Document document = Document.newDefaultHtml(response.code().toString());
        formatDocument(document, response, failure);
        response.setHTML(document);
    }

    private static void formatXHtml(HttpResponse.Editable response, HttpRequestFailure failure) {
        Document document = Document.newDefaultXhtml(response.code().toString());
        formatDocument(document, response, failure);
        response.setXML(document, XML.XHTML);
        response.setContentType(ContentType.XHTML);
    }

    private static void formatDocument(Document document, HttpResponse.Editable response, HttpRequestFailure failure) {
        document.getElementByTag("head").children.add(new Node("style", new Text(DEFAULT_CSS)));
        Node body = document.getElementByTag("body");
        body.children.add(new Node("h1", new Text(response.code().toString())));
        if(failure.message() != null)
            body.children.add(new Node("p", new Text(failure.message())));
        if(failure.detail() != null) {
            Node text;
            if(failure.detail() instanceof JsonStructure)
                text = new Node("pre", new Text(Json.toString(failure.detail(), true)));
            else text = new Node("i", new Text(failure.detail().toString()));
            body.children.add(new Node("p", text));
        }
    }
}
