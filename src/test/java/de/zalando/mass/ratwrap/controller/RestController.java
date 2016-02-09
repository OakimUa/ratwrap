package de.zalando.mass.ratwrap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.annotation.*;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.handler.HandlerDispatcher;
import de.zalando.mass.ratwrap.enums.RequestMethod;
import org.springframework.http.ResponseEntity;
import ratpack.handling.Context;
import ratpack.http.client.HttpClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RequestController(uri = "test/{param1}/")
public class RestController {
    @RequestHandler(method = RequestMethod.GET, uri = "path/{param2}")
    public Map<String, Object> doGet(
            @PathVariable("param1") final String param1,
            @PathVariable("param2") final Integer param2,
            @QueryParam("param3") final String param3,
            @HeaderParam("param4") final String param4) {
        final Map<String, Object> data = new HashMap<>();
        data.put("param1", param1);
        data.put("param2", param2);
        data.put("param3", param3);
        data.put("param4", param4);
        return data;
    }

    @RequestHandler(method = RequestMethod.GET, uri = "path202/{param2}", status = 202)
    public Map<String, Object> doGet202(
            @PathVariable("param1") final String param1,
            @PathVariable("param2") final Integer param2) {
        final Map<String, Object> data = new HashMap<>();
        data.put("param1", param1);
        data.put("param2", param2);
        return data;
    }

    @RequestHandler(method = RequestMethod.POST, uri = "path/{param2}")
    public ResponseEntity<Void> doCreate(
            @PathVariable("param1") final String param1,
            @PathVariable("param2") final Integer param2,
            InputData input) {
        return ResponseEntity.created(URI.create(String.join("/",
                "path/to/entity",
                param1,
                param2.toString(),
                input.getField1(),
                input.getField2().toString()))).build();
    }

    @RequestHandler(method = RequestMethod.GET, uri = "context", produce = "text/plain")
    public String doGetWithContext(
            @ContextParam final HttpClient httpClient,
            @ContextParam final ObjectMapper objectMapper,
            @ContextParam final Context ctx) {
        return "HttpClient=" + (httpClient != null) + "|ObjectMapper=" + (objectMapper != null) + "|Context=" + (ctx != null);
    }

    @RequestHandler(method = RequestMethod.GET, uri = "registry", produce = "text/plain")
    public Boolean doGetRegistryCheck(
            @ContextParam final ObjectMapper objectMapper,
            @ContextParam final HandlerDispatcher dispatcher,
            @ContextParam final TestRegistryBean testRegistryBean,
            @ContextParam final AnotherTestRegistryBean anotherTestRegistryBean) {
        return objectMapper!=null &&
                dispatcher != null &&
                testRegistryBean != null &&
                anotherTestRegistryBean != null;
    }
}
