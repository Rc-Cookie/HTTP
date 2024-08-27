package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Console;

class RootProcessor implements HttpProcessor {

    public HttpErrorFormatter errorFormatter = HttpErrorFormatter.DEFAULT;

    @Override
    public void postprocess(HttpResponse.Editable response) {
        HttpRequest request = response.request();
        if(!(request instanceof HttpRequest.Received)) return;
        try {
            ((HttpRequest.Received) request).getResponseConfigurators().accept(response);
            // Only clear on success, if failed (or redirected etc.) they will run (once) more on the error response
            ((HttpRequest.Received) request).clearResponseConfigurators();
        } catch(HttpControlFlowException flow) {
            processControlFlow((HttpRequest.Received) response.request(), flow);
        } catch(Exception e) {
            processError((HttpRequest.Received) response.request(), e);
        }
    }

    @Override
    public void processControlFlow(HttpRequest.Received request, HttpControlFlowException flow) {
        try {
            if(flow instanceof HttpRequestFailure) {
                HttpRequestFailure failure = (HttpRequestFailure) flow;
                if(failure.code() == ResponseCode.INTERNAL_SERVER_ERROR && failure.getCause() != null)
                    Console.error(failure.getCause());

                HttpResponse.Editable response = request.respond(failure.code());
                try {
                    failure.format(errorFormatter, response);
                } catch(Exception e) {
                    Console.error("Error in error formatter:");
                    Console.error(e);
                    HttpErrorFormatter.DEFAULT.format(response, failure);
                }
                try {
                    request.getResponseConfigurators().accept(response);
                    request.clearResponseConfigurators();
                } catch(HttpControlFlowException f) {
                    // Prevent stack overflows caused by configurators always redirecting or similar
                    request.clearResponseConfigurators();
                    processControlFlow(request, HttpRequestFailure.internal(new RuntimeException("Response configurator threw HttpControlFlowException while configuring an error response, which is not allowed:"+f, f)));
                } catch(Exception e) {
                    request.clearResponseConfigurators();
                    processControlFlow(request, HttpRequestFailure.internal(new RuntimeException("Error in response configurator while configuring error response:"+e, e)));
                }
            }
            else flow.format(request.respond());
        } catch(Exception e) {
            Console.error(flow instanceof HttpRequestFailure ? "Error in default error formatter:" : "Error in control flow response formatting:");
            Console.error(e);
            e.printStackTrace();
            HttpErrorFormatter.DEFAULT.format(request.respond(ResponseCode.INTERNAL_SERVER_ERROR), HttpRequestFailure.internal(e));
        }
    }

    @Override
    public void processError(HttpRequest.Received request, Exception exception) {
        processControlFlow(request, HttpRequestFailure.internal(exception));
    }
}
