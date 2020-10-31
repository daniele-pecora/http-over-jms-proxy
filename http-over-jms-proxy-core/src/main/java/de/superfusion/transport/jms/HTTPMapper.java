package de.superfusion.transport.jms;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.*;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.DefaultHttpResponseFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Helper for mapping http requests and http responses to be sent as jms messages.<br/>
 *
 * @author daniele
 */
public class HTTPMapper {

    private static final String ENCODING = "UTF-8";
    private static final Charset CHARSET_ENCODING = Charset.forName(ENCODING);
    private static final boolean USE_BASE64_PAYLOAD = true;

    private static Gson GSON = new GsonBuilder()
            /**
             * Use {@link GsonBuilder#setLenient()}
             * Fix:
             * <pre>
             *     Caused by: com.google.gson.stream.MalformedJsonException:
             *     Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1254 path $
             * </pre>
             */
            .setLenient()
            .create();

    /**
     * Simple pojo to hold a http request.<br/>
     *
     * @author daniele
     */
    public static class SimplePlainHTTPRequest implements Serializable {
        public String id;
        public Date timestamp;
        public String groupName;
        public String uri;
        public String method;
        public List<HeaderVar> headers;
        public String payload;
        public String protocol;
        public String servletPath;
        public String contextPath;
        public String pathInfo;
        public String queryString;
        public String requestURL;

        public static SimplePlainHTTPRequest fromJson(String json) {
            return GSON.fromJson(json, SimplePlainHTTPRequest.class);
        }

        public String toJson() {
            return GSON.toJson(this);
        }
    }

    /**
     * Simple pojo to hold a http response.<br/>
     *
     * @author daniele
     */
    static class SimplePlainHTTPResponse implements Serializable {
        public Date timestamp;
        public String reasonPhrase;
        public int status = -1;
        public List<HeaderVar> headers;
        public String protocol;
        public String payload;

        public static SimplePlainHTTPResponse fromJson(String json) {
            return GSON.fromJson(json, SimplePlainHTTPResponse.class);
        }

        public String toJson() {
            return GSON.toJson(this);
        }
    }

    private static byte[] encodePayload(String content) {
        if (null == content)
            return null;
        if (USE_BASE64_PAYLOAD) {
            try {
                return encodePayload(content.getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return content.getBytes();
    }

    private static byte[] encodePayload(byte[] bytes) {
        if (null == bytes)
            return null;
        if (USE_BASE64_PAYLOAD) {
            return Base64.getEncoder().encode(bytes);
        }
        return bytes;
    }

    private static byte[] decodePayload(String content) {
        if (null == content)
            return null;
        if (USE_BASE64_PAYLOAD) {
            try {
                return decodePayload(content.getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return content.getBytes();
    }

    private static byte[] decodePayload(byte[] bytes) {
        if (null == bytes)
            return null;
        if (USE_BASE64_PAYLOAD) {
            return Base64.getDecoder().decode(bytes);
        }
        return bytes;
    }

    private static HttpRequest createHTTPRequest(SimplePlainHTTPRequest sRequest) {
        String method = sRequest.method;
        String uri = sRequest.uri;
        RequestBuilder builder = RequestBuilder.create(method);
        builder.setUri(uri);
//        builder.setCharset(CHARSET_ENCODING);
        HttpVersion version = getHttpVersion(sRequest.protocol);
        builder.setVersion(version);
        if (sRequest.payload != null && sRequest.payload.length() > 0) {
            final byte[] bytes = decodePayload(sRequest.payload);
            builder.setEntity(createEntity(bytes));
        }
        if (null != sRequest.headers)
            for (HeaderVar headerVar : sRequest.headers) {
                builder.addHeader(headerVar.getName(), headerVar.getValue());
            }
        return builder.build();
    }

    private static HttpVersion getHttpVersion(String string) {
        if (null != string)
            switch (string) {
                case "HTTP/1.0":
                    return HttpVersion.HTTP_1_0;
                case "HTTP/1.1":
                    return HttpVersion.HTTP_1_1;
                case "HTTP/2":
                    return new HttpVersion(2, 0);
                case "HTTP/2.0":
                    return new HttpVersion(2, 0);
            }
        return HttpVersion.HTTP_1_1;
    }

    private static HttpEntity createEntity(byte[] bytes) {
        final ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        return new HttpEntity() {
            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public boolean isChunked() {
                return false;
            }

            @Override
            public long getContentLength() {
                return bytes.length;
            }

            @Override
            public Header getContentType() {
                return null;
            }

            @Override
            public Header getContentEncoding() {
                return null;
            }

            @Override
            public InputStream getContent() throws IOException, UnsupportedOperationException {
                return bi;
            }

            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                outputStream.write(bytes);
            }

            @Override
            public boolean isStreaming() {
                return false;
            }

            @Override
            public void consumeContent() throws IOException {

            }
        };
    }

    private static HttpResponse createHTTPResponse(SimplePlainHTTPResponse sResponse) {
        int status = sResponse.status;
        String reasonPhrase = sResponse.reasonPhrase;
        HttpVersion httpVersion = getHttpVersion(sResponse.protocol);
        HttpResponse response = DefaultHttpResponseFactory.INSTANCE
                .newHttpResponse(new ProtocolVersion(HttpVersion.HTTP, httpVersion.getMajor(), httpVersion.getMinor()), status, null);
        final byte[] bytes = decodePayload(sResponse.payload.getBytes());
        response.setEntity(createEntity(bytes));
        response.setReasonPhrase(reasonPhrase);
        for (HeaderVar headerVar : sResponse.headers) {
            response.addHeader(headerVar.getName(), headerVar.getValue());
        }
        return response;
    }

    /**
     * Convert <code>request</code> to string to be sent as jms message.<br/>
     *
     * @param request
     * @param requestURL
     * @param servletPath
     * @param contextPath
     * @param pathInfo
     * @param queryString
     * @return
     */
    public static String stringify(HttpRequest request, String requestURL, String servletPath, String contextPath, String pathInfo, String queryString) {
        SimplePlainHTTPRequest si = null;
        try {
            si = createSimpleHTTPRequest(request);
            si.servletPath = servletPath;
            si.contextPath = contextPath;
            si.pathInfo = pathInfo;
            si.queryString = queryString;
            si.requestURL = requestURL;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new GsonBuilder().create().toJson(si);
    }

    /**
     * JSON parsing
     *
     * @param clazz
     * @param value
     * @param <T>
     * @return
     */
    public static <T> T parseJSON(Class<T> clazz, String value) {
        return GSON.fromJson(value, clazz);
    }

    private static String stringify(HttpResponse response) {
        SimplePlainHTTPResponse si = null;
        try {
            si = createSimpleHTTPResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new GsonBuilder().create().toJson(si);
    }

    /**
     * Convert {@link HttpResponse} to {@link SimplePlainHTTPRequest}
     *
     * @param response
     * @return
     * @throws IOException
     */
    static SimplePlainHTTPResponse createSimpleHTTPResponse(HttpResponse response) throws IOException {
        SimplePlainHTTPResponse si = new SimplePlainHTTPResponse();
        si.timestamp = new Date();
        try {
            si.payload = createPayload(response);
        } catch (IOException e) {
            System.err.println("Error creating response payload. Check webapp firewall settings.");
            e.printStackTrace();
        }
        si.headers = createHeaders(response);
        si.status = response.getStatusLine().getStatusCode();
        si.reasonPhrase = response.getStatusLine().getReasonPhrase();
        si.protocol = createProtocolVersion(response.getProtocolVersion());
        return si;
    }

    private static String createPayload(HttpResponse response) throws IOException {
        String payload = "";
        if (null != response.getEntity() && null != response.getEntity().getContent()) {
            if (USE_BASE64_PAYLOAD) {
                payload = ByteSource.wrap(encodePayload(ByteStreams.toByteArray(response.getEntity().getContent())))
                        .asCharSource(CHARSET_ENCODING)
                        .read();
            } else {
                payload = ByteSource.wrap(ByteStreams.toByteArray(response.getEntity().getContent()))
                        .asCharSource(CHARSET_ENCODING)
                        .read();
            }
        }
        return payload;
    }

    private Random RANDOM = new Random(System.nanoTime());

    private String createRequestId(HttpRequest request) {
        return String.valueOf(RANDOM.nextLong() + "_" + request.getClass().getName() + "@" + Integer.toHexString(hashCode()));
    }

    private static SimplePlainHTTPRequest createSimpleHTTPRequest(HttpRequest request) throws IOException {
        SimplePlainHTTPRequest si = new SimplePlainHTTPRequest();
        si.id = new HTTPMapper().createRequestId(request);
        si.timestamp = new Date();
        si.uri = createRequestUri(request);
        si.method = request.getRequestLine().getMethod();
        si.headers = createHeaders(request);
        si.payload = createPayload(request);
        si.groupName = createRequestGroupName(si.uri);
        si.protocol = createProtocolVersion(request.getProtocolVersion());
        return si;
    }

    private static String createProtocolVersion(ProtocolVersion protocolVersion) {
        if (null != protocolVersion)
            return protocolVersion.toString();
        return null;
    }

    private static String createPayload(HttpRequest request) throws IOException {
        String payload = null;
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) request;
            if (USE_BASE64_PAYLOAD) {
                payload = ByteSource.wrap(encodePayload(ByteStreams.toByteArray(r.getEntity().getContent())))
                        .asCharSource(CHARSET_ENCODING)
                        .read();
            } else {
                payload = ByteSource.wrap(ByteStreams.toByteArray(r.getEntity().getContent()))
                        .asCharSource(CHARSET_ENCODING)
                        .read();
            }
        }
        return payload;
    }

    /**
     * Representation of an HTTP Header Parameter
     */
    static class HeaderVar implements Serializable {

        private static final long serialVersionUID = -4541621113232649179L;
        private String name;
        private String value;

        public HeaderVar(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    private static List<HeaderVar> createHeaders(HttpRequest request) {
        return createHeaders(request.getAllHeaders());
    }

    private static List<HeaderVar> createHeaders(HttpResponse response) {
        return createHeaders(response.getAllHeaders());
    }

    private static List<HeaderVar> createHeaders(Header[] allHeaders) {
        List<HeaderVar> headers = new ArrayList<>();
        for (Header headerParam : allHeaders) {
            String name = headerParam.getName();
            String value = headerParam.getValue();
            headers.add(new HeaderVar(name, value));
        }
        return headers;
    }

    private static String createRequestGroupName(String requestUri) {
        return requestUri
                .replaceFirst("\\?.*", "")      // Replace query parameters.
                .replaceFirst("^/", "")         // Replace the initial slash.
                .replaceAll("/", ".");          // Replace all slashes with dots.
    }

    private static String createRequestUri(HttpRequest request) {
        String uri = request.getRequestLine().getUri();
        return uri;
    }

    public static String responseToJson(HttpResponse response) throws IOException {
        SimplePlainHTTPResponse simplePlainHTTPResponse = createSimpleHTTPResponse(response);
        return simplePlainHTTPResponse.toJson();
    }

    /**
     * Convert a json string to  {@link SimplePlainHTTPResponse}
     *
     * @param SimplePlainHTTPResponseJson
     * @return
     */
    public static HttpResponse responseFromJson(String SimplePlainHTTPResponseJson) {
        SimplePlainHTTPResponse simplePlainHTTPResponse = SimplePlainHTTPResponse.fromJson(SimplePlainHTTPResponseJson);
        return createHTTPResponse(simplePlainHTTPResponse);
    }

    /**
     * Convert a {@link HttpRequest} string to {@link String}
     *
     * @param request
     * @return
     * @throws IOException
     */
    public static String requestToJson(HttpRequest request) throws IOException {
        SimplePlainHTTPRequest simplePlainHTTPRequest = createSimpleHTTPRequest(request);
        return simplePlainHTTPRequest.toJson();
    }

    /**
     * Convert a json string to {@link HttpRequest}
     *
     * @param SimplePlainHTTPRequestJson
     * @return
     */
    public static HttpRequest requestFromJson(String SimplePlainHTTPRequestJson) {
        SimplePlainHTTPRequest simplePlainHTTPRequest = SimplePlainHTTPRequest.fromJson(SimplePlainHTTPRequestJson);
        return createHTTPRequest(simplePlainHTTPRequest);
    }

    /**
     * Read a resource defined in <code>resourceName</code> to String.<br/>
     *
     * @param resourceName
     * @return
     */
    static String getJsonResponse(String resourceName) {
        try {
            InputStream i = HTTPMapper.class.getResourceAsStream(resourceName);
            return ByteSource.wrap(ByteStreams.toByteArray(i))
                    .asCharSource(CHARSET_ENCODING)
                    .read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
