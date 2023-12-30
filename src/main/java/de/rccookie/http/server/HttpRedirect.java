package de.rccookie.http.server;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.rccookie.http.ContentType;
import de.rccookie.http.Header;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.json.JsonArray;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import de.rccookie.xml.Text;
import de.rccookie.xml.XML;
import org.jetbrains.annotations.Nullable;

/**
 * A special type of {@link RuntimeException} thrown to indicate that
 * a redirect response should be sent. These types of exceptions are caught when
 * thrown from an http handler by the server and automatically cause a response
 * to be sent.
 *
 * <p>Despite being exceptions, this class will not log its stack trace when
 * thrown, which results in considerably faster runtime. They should thus <b>not</b>
 * be used in places where an actual error occurred in the code.</p>
 */
public abstract class HttpRedirect extends HttpControlFlowException {

    private final ResponseCode code;

    protected HttpRedirect(ResponseCode code) {
        super(null, null, false, false);
        if(code.type() != ResponseCode.Type.REDIRECT)
            throw new IllegalArgumentException("Redirect response code expected, got "+code);
        this.code = code;
    }

    @Override
    public String getMessage() {
        return code.name();
    }

    @Override
    public String getLocalizedMessage() {
        return code.name();
    }

    public ResponseCode code() {
        return code;
    }

    public void format(HttpResponse.Sendable response) {
        response.setCode(code);
        doFormat(response);
    }

    protected abstract void doFormat(HttpResponse.Sendable response);


    public static HttpRedirect multipleChoices(Consumer<? super HttpResponse.Sendable> formatter) {
        return new HttpRedirect(ResponseCode.MULTIPLE_CHOICE) {
            @Override
            public void doFormat(HttpResponse.Sendable response) {
                formatter.accept(response);
            }
        };
    }

    public static HttpRedirect multipleChoices(Map<? extends String, ? extends String> choices, @Nullable String preferred) {
        Arguments.checkNull(choices, "choices");
        return new HttpRedirect(ResponseCode.MULTIPLE_CHOICE) {
            @Override
            public void doFormat(HttpResponse.Sendable response) {
                choices.forEach((url,$) -> response.addHeaderField("Link", url+"; rel=\"alternative\""));
                if(preferred != null)
                    response.setHeaderField("Location", preferred);

                ContentType returnType = response.request().accept().getPreferred(ContentType.JSON, ContentType.HTML, ContentType.XHTML, ContentType.XML);
                if(returnType == ContentType.JSON) {
                    JsonArray arr = new JsonArray();
                    choices.forEach((url,name) -> {
                        if(name == null)
                            arr.add(url);
                        else arr.add(new JsonObject("name", name, "url", url));
                    });
                    response.setJson(new JsonObject("choices", arr));
                }
                else if(returnType == ContentType.HTML || returnType == ContentType.XHTML) {
                    Node list = new Node("ul");
                    choices.forEach((url,name) -> {
                        Node a = new Node("a");
                        a.attributes.put("href", url);
                        a.children.add(new Text(name != null ? name : url));
                        list.children.add(a);
                    });
                    if(returnType == ContentType.HTML)
                        response.setHTML(Document.newDefaultHtml(list));
                    else response.setXML(Document.newDefaultXhtml("Options", list), XML.XHTML);
                }
                else {
                    Node list = new Node("choices");
                    choices.forEach((url,name) -> {
                        Node choice = new Node("choice", new Text(url));
                        if(name != null)
                            choice.attributes.put("name", name);
                        list.children.add(choice);
                    });
                    response.setXML(list);
                }
            }
        };
    }

    public static HttpRedirect multipleChoices(Map<? extends URL, ? extends String> choices, @Nullable URL preferred) {
        return multipleChoices(choices.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)), preferred != null ? preferred.toString() : null);
    }

    public static HttpRedirect multipleChoices(Collection<? extends String> choices, @Nullable String preferred) {
        Map<String, String> map = new HashMap<>();
        for(String choice : Arguments.checkNull(choices, "choices"))
            map.put(choice, null);
        return multipleChoices(map, preferred);
    }

    public static HttpRedirect multipleChoices(Collection<? extends URL> choices, @Nullable URL preferred) {
        Map<String, String> map = new HashMap<>();
        for(URL choice : Arguments.checkNull(choices, "choices"))
            map.put(choice.toString(), null);
        return multipleChoices(map, preferred != null ? preferred.toString() : null);
    }


    public static HttpRedirect movedPermanently(String toUrl) {
        return new ToLocation(ResponseCode.MOVED_PERMANENTLY, Arguments.checkNull(toUrl, "toUrl"));
    }

    public static HttpRedirect movedPermanently(URL toUrl) {
        return new ToLocation(ResponseCode.MOVED_PERMANENTLY, Arguments.checkNull(toUrl, "toUrl"));
    }


    public static HttpRedirect found(String atUrl) {
        return new ToLocation(ResponseCode.FOUND, Arguments.checkNull(atUrl, "atUrl"));
    }

    public static HttpRedirect found(URL atUrl) {
        return new ToLocation(ResponseCode.FOUND, Arguments.checkNull(atUrl, "atUrl"));
    }


    public static HttpRedirect seeOther(String url) {
        return new ToLocation(ResponseCode.SEE_OTHER, Arguments.checkNull(url, "url"));
    }

    public static HttpRedirect seeOther(URL url) {
        return new ToLocation(ResponseCode.SEE_OTHER, Arguments.checkNull(url, "url"));
    }


    public static HttpRedirect notModified(Consumer<? super Header> headerConfigurator) {
        Arguments.checkNull(headerConfigurator, "headerConfigurator");
        return new HttpRedirect(ResponseCode.NOT_MODIFIED) {
            @Override
            public void doFormat(HttpResponse.Sendable response) {
                headerConfigurator.accept(response.header());
            }
        };
    }


    public static HttpRedirect temporaryRedirect(String toUrl) {
        return new ToLocation(ResponseCode.TEMPORARY_REDIRECT, Arguments.checkNull(toUrl, "toUrl"));
    }

    public static HttpRedirect temporaryRedirect(URL toUrl) {
        return new ToLocation(ResponseCode.TEMPORARY_REDIRECT, Arguments.checkNull(toUrl, "toUrl"));
    }


    public static HttpRedirect permanentRedirect(String toUrl) {
        return new ToLocation(ResponseCode.PERMANENT_REDIRECT, Arguments.checkNull(toUrl, "toUrl"));
    }

    public static HttpRedirect permanentRedirect(URL toUrl) {
        return new ToLocation(ResponseCode.PERMANENT_REDIRECT, Arguments.checkNull(toUrl, "toUrl"));
    }



    private static final class ToLocation extends HttpRedirect {

        private final String location;

        private ToLocation(ResponseCode code, String location) {
            super(code);
            this.location = Arguments.checkNull(location, "location");
        }
        private ToLocation(ResponseCode code, URL location) {
            this(code, location.toString());
        }

        @Override
        public void doFormat(HttpResponse.Sendable response) {
            response.setHeaderField("Location", location);
        }
    }
}
