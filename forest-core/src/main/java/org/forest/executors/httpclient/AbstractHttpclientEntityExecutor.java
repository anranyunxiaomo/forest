package org.forest.executors.httpclient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.forest.executors.httpclient.body.HttpclientBodyBuilder;
import org.forest.executors.httpclient.response.HttpclientForestResponseFactory;
import org.forest.handler.ResponseHandler;
import org.forest.http.ForestRequest;
import org.forest.http.ForestResponse;
import org.forest.exceptions.ForestNetworkException;
import org.forest.exceptions.ForestRuntimeException;
import org.forest.http.ForestResponseFactory;

import java.io.*;

/**
 * @author gongjun[dt_flys@hotmail.com]
 * @since 2017-04-20 13:22
 */
public abstract class AbstractHttpclientEntityExecutor<T extends HttpEntityEnclosingRequestBase> extends AbstractHttpclientExecutor<T> {

    private static Log log = LogFactory.getLog(HttpclientPostExecutorHttpclient.class);

    public AbstractHttpclientEntityExecutor(HttpclientConnectionManager connectionManager, ForestRequest requst) {
        super(connectionManager, requst);
    }

    protected void prepareBodyBuilder() {
        bodyBuilder = new HttpclientBodyBuilder<>();
    }

    @Override
    protected String getLogContentForBody(T httpReq) {
        try {
            InputStream in = httpReq.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuffer buffer = new StringBuffer();
            String line;
            String body;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + " ");
            }
            body = buffer.toString();
            return body;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public void execute(int retryCount, ResponseHandler responseHandler) {
        try {
            logRequestBegine(retryCount, httpRequest);
            HttpResponse httpResponse = sendRequest(client, httpRequest);
            ForestResponseFactory forestResponseFactory = new HttpclientForestResponseFactory();
            response = forestResponseFactory.createResponse(request, httpResponse);
            logResponse(request, client, httpRequest, response);
            if (response.isError()) {
                throw new ForestNetworkException(httpResponse.getStatusLine().getReasonPhrase(), response.getStatusCode());
            }
        } catch (IOException e) {
            if (retryCount >= request.getRetryCount()) {
                httpRequest.abort();
                throw new RuntimeException(e);
            }
            log.error(e.getMessage());
            execute(retryCount + 1, responseHandler);
        } catch (ForestRuntimeException e) {
            httpRequest.abort();
            throw e;
        }
    }
}
