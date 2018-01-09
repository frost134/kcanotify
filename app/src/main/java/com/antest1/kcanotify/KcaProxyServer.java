package com.antest1.kcanotify;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import android.content.Context;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class KcaProxyServer {
    private static boolean is_on;
    private static HttpProxyServer proxyServer;
    private static final int PORT = 24110;
    public static Context cntx;
    public static Handler handler;

    public static final int KC_REFERER_APP = 1;
    public static final int KC_REFERER_BROWSER = 2;

    private static final String KCA_HOST = "203.104.209.7";
    private static final String KC_APP_REFERER = "app:/AppMain.swf/";
    private static final String KC_BROWSER_REFERER = "kcs/mainD2.swf";
    private static final String HTTP_CONTENT_ENCODING = HttpHeaders.Names.CONTENT_ENCODING;

    private static final String KCA_USERAGENT = String.format("Kca/%s ", BuildConfig.VERSION_NAME);
    private static final String KC_NEAT_HOST = "kancolle-neat.rhcloud.com";
    private static final String KC_NEAT_VIEW = "view_kancolle.php";

    public static KcaRequest2 kcaRequest = new KcaRequest2();
    public static ExecutorService executorService = Executors.newScheduledThreadPool(15);

    public static String currentRefererInfo = "";

    public static int getReferer() {
        if (currentRefererInfo.contains(KC_APP_REFERER)) {
            return KC_REFERER_APP;
        } else if(currentRefererInfo.contains(KC_BROWSER_REFERER)){
            return KC_REFERER_BROWSER;
        } else {
            return 0;
        }
    }

    public KcaProxyServer() {
        proxyServer = null;
    }

    public static void start(Handler h) {
        handler = h;
        if (proxyServer == null) {
            HttpFiltersSource filtersSource = getFiltersSource();
            proxyServer = DefaultHttpProxyServer.bootstrap().withPort(PORT).withAllowLocalOnly(false)
                    .withConnectTimeout(20000).withFiltersSource(filtersSource).withName("FilterProxy").start();
            Log.e("KCA", "Start");
        }
    }

    public static void stop() {
        handler = null;
        if (is_on()) {
            proxyServer.abort();
            proxyServer = null;
        }
        // //Log.e("KCA", "Stop");
    }

    public static boolean is_on() {
        return proxyServer != null;
    }

    private static HttpFiltersSource getFiltersSource() {
        return new HttpFiltersSourceAdapter() {
            @Override
            public int getMaximumRequestBufferSizeInBytes() { return 256 * 1024 * 1024; }

            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest) {

                return new HttpFiltersAdapter(originalRequest) {
                    boolean isKcsApi = false;
                    boolean isKcsRes = false;
                    boolean isKcaRes = false;
                    boolean isKcaVer = false;
                    boolean is_kca = false;

                    String currentUrl = "";

                    @Override
                    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                        if (httpObject instanceof FullHttpRequest) {
                            is_kca = false;
                            FullHttpRequest request = (FullHttpRequest) httpObject;
                            String requestUri = request.getUri();

                            if(request.headers().contains(HttpHeaders.Names.ACCEPT_ENCODING)) {
                                String acceptEncodingData = request.headers().get(HttpHeaders.Names.ACCEPT_ENCODING).trim();
                                if (acceptEncodingData.endsWith(",")) {
                                    acceptEncodingData = acceptEncodingData.concat(" sdch");
                                    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, acceptEncodingData);
                                }
                            }

                            String[] requestData = request.toString().split("\n");
                            String requestHeaderString = "";
                            boolean isKcRequest = false;
                            boolean isNeatRequest = false;
                            isKcaVer = requestUri.startsWith("/kca/version");
                            isKcsApi = requestUri.startsWith("/kcsapi/api_");
                            isKcsRes = requestUri.startsWith("/kcs/resources");
                            isKcaRes = requestUri.startsWith("/kca/resources");

                            boolean isTestRequest = false;
                            boolean isInternalRequest = false;

                            Iterator<Map.Entry<String, String>> requestHeader = request.headers().iterator();
                            while (requestHeader.hasNext()) {
                                Map.Entry<String, String> data = requestHeader.next();
                                String key = data.getKey();
                                String value = data.getValue();
                                requestHeaderString += key + ": " + value + "\r\n";
                                if (key.equals(HttpHeaders.Names.REFERER)) {
                                    if (value.startsWith(KC_APP_REFERER) || value.contains(KC_BROWSER_REFERER)) {
                                        isKcRequest = true;
                                    }
                                }
                                if (key.equals(HttpHeaders.Names.HOST)) {
                                    if (value.startsWith(KCA_HOST)) {
                                        isKcRequest = true;
                                    }
                                    if(value.contains(KC_NEAT_HOST)) {
                                        isNeatRequest = true;
                                    }
                                }
                                if (key.equals(HttpHeaders.Names.USER_AGENT)) {
                                    if (value.contains(KCA_USERAGENT)) {
                                        isInternalRequest = true;
                                    }
                                }
                            }

                            if (isInternalRequest) {
                                Log.e("KCA", "Request(I) " + request.getUri());
                                String useragent = HttpHeaders.getHeader(request, HttpHeaders.Names.USER_AGENT).replaceAll(KCA_USERAGENT, "").trim();
                                request.headers().set(HttpHeaders.Names.USER_AGENT, useragent);
                                return null;
                            }

                            //if (isKcRequest  && !isInternalRequest && (isKcaVer || isKcsApi || isKcsRes || isKcaRes || isKcaApiS2)) {
                            if (isKcRequest  && !isInternalRequest && (isKcsApi || isKcaVer)) {
                                Log.e("KCA", "Request " + request.getUri());

                                ByteBuf contentBuf = request.content();
                                boolean gzipped = false;
                                byte[] requestBody = new byte[contentBuf.readableBytes()];
                                int readerIndex = contentBuf.readerIndex();
                                contentBuf.getBytes(readerIndex, requestBody);
                                String requestBodyStr = new String(requestBody);
                                currentRefererInfo = request.headers().get(HttpHeaders.Names.REFERER);
                                try {
                                    String responseData = kcaRequest.post(request.getUri(), requestHeaderString, requestBodyStr);

                                    JsonObject responseObject = new JsonParser().parse(responseData).getAsJsonObject();
                                    String responseHeader = responseObject.get("header").getAsString();
                                    int statusCode = responseObject.get("status").getAsInt();

                                    Log.e("KCA", String.valueOf(statusCode));
                                    byte[] responseBody = Base64.decode(responseObject.get("data").getAsString(), Base64.DEFAULT);
                                    ByteBuf buffer = Unpooled.wrappedBuffer(responseBody);
                                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buffer);
                                    for (String line: responseHeader.split("\r\n")) {
                                        String[] entry = line.trim().split(": ");
                                        HttpHeaders.setHeader(response, entry[0], entry[1]);
                                    }
                                    //HttpHeaders.setContentLength(response, buffer.readableBytes());
                                    //HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/html");
                                    is_kca = true;
                                    currentUrl = requestUri;

                                    if (response.headers().contains(HTTP_CONTENT_ENCODING)) {
                                        if (response.headers().get(HTTP_CONTENT_ENCODING).startsWith("gzip")) {
                                            gzipped = true;
                                            responseBody = KcaUtils.gzipdecompress(responseBody);
                                        }
                                    }

                                    KcaHandler k = new KcaHandler(handler, currentUrl, requestBody, responseBody);
                                    executorService.execute(k);

                                    return response;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(isNeatRequest && requestUri.contains(KC_NEAT_VIEW)) {
                                ByteBuf contentBuf = request.content();
                                boolean gzipped = false;
                                byte[] requestBody = new byte[contentBuf.readableBytes()];
                                int readerIndex = contentBuf.readerIndex();
                                contentBuf.getBytes(readerIndex, requestBody);
                                String requestBodyStr = new String(requestBody);
                                currentRefererInfo = request.headers().get(HttpHeaders.Names.REFERER);
                                try {
                                    String responseData = kcaRequest.post(request.getUri(), requestHeaderString, requestBodyStr);
                                    // Replace for phone

                                    JsonObject responseObject = new JsonParser().parse(responseData).getAsJsonObject();
                                    String responseHeader = responseObject.get("header").getAsString();
                                    int statusCode = responseObject.get("status").getAsInt();

                                    Log.e("KCA", String.valueOf(statusCode));
                                    byte[] responseBody = Base64.decode(responseObject.get("data").getAsString(), Base64.DEFAULT);
                                    String bodyContent = new String(KcaUtils.gzipdecompress(responseBody));

                                    bodyContent = bodyContent.replace("<title>", "<meta name=\"viewport\" content=\"width=850,user-scalable=no\"/>\n<title>");
                                    bodyContent = bodyContent.replace("<style>", "<style>body {background-color: black;}\n");
                                    bodyContent = bodyContent.replace("margin-top:-16px; margin-left:-50px;", "margin-top:-16px;");
                                    bodyContent = bodyContent.replace("width=\"900\" height=\"1200\"", "width=\"800\" height=\"495\"");

                                    Log.e("KCA", bodyContent);

                                    responseBody = KcaUtils.gzipcompress(bodyContent);
                                    ByteBuf buffer = Unpooled.wrappedBuffer(responseBody);
                                    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), buffer);
                                    for (String line: responseHeader.split("\r\n")) {
                                        String[] entry = line.trim().split(": ");
                                        HttpHeaders.setHeader(response, entry[0], entry[1]);
                                    }
                                    //HttpHeaders.setContentLength(response, buffer.readableBytes());
                                    //HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/html");
                                    currentUrl = requestUri;
                                    return response;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        return null;
                    }

                    @Override
                    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
                        //Log.e("KCA", "proxyToServerConnectionSucceeded");
                        ChannelPipeline pipeline = serverCtx.pipeline();
                        /*
						if (pipeline.get("inflater") != null) {
							// Log.e("KCA", "remove inflater");
							pipeline.remove("inflater");
						}*/
                        /*
						 if (pipeline.get("aggregator") != null) {
                            Log.e("KCA", "remove aggregator");
                            pipeline.remove("aggregator");
                         }
                         */
                        super.proxyToServerConnectionSucceeded(serverCtx);
                    }
                };
            }
        };
    }

}
