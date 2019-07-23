/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sony.internal.transports;

import java.io.Closeable;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.sony.internal.SonyBindingConstants;
import org.openhab.binding.sony.internal.net.HttpResponse;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebRequest;
import org.openhab.binding.sony.internal.scalarweb.models.ScalarWebResult;

/**
 *
 *
 * @author Tim Roberts - Initial contribution
 */
@NonNullByDefault
public interface SonyTransport extends Closeable {
   String AUTO = "auto";
   String HTTP = "xhrpost:jsonizer";
   String WEBSOCKET = "websocket:jsonizer";

   /**
    * Execute the specified json request with the specified HTTP headers
    *
    * @param jsonRequest the non-null, non-empty command to execute (json or xml body)
    * @param headers     the possibly null, possibly empty list of headers to include (dependent on the transport)
    * @return the non-null future result
    */
   public CompletableFuture<? extends TransportResult> execute(TransportPayload payload,
         TransportOption... options);

   public void setOption(TransportOption option);
   public void removeOption(TransportOption option);

   public void addListener(SonyTransportListener listener);

   public boolean removeListener(SonyTransportListener listener);

   public String getProtocolType();

   public URL getBaseUrl();
   
   @Override
   public void close();

   public default HttpResponse executeGet(String url, TransportOption... options) {
      try {
         final TransportResult result = execute(new TransportPayloadHttp(url), TransportOption.concat(options, TransportOptionMethod.GET))
               .get(SonyBindingConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
         if (result instanceof TransportResultHttpResponse) {
            return ((TransportResultHttpResponse) result).getResponse();
         } else {
            return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                  "Execution of " + url
                        + " didn't return a TransportResultHttpResponse: "
                        + result.getClass().getName());
         }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
               "Execution of " + url + " threw an exception: " + e.getMessage());

      }
   }

   public default HttpResponse executeDelete(String url, TransportOption... options) {
      try {
         final TransportResult result = execute(new TransportPayloadHttp(url), TransportOption.concat(options, TransportOptionMethod.DELETE))
               .get(SonyBindingConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
         if (result instanceof TransportResultHttpResponse) {
            return ((TransportResultHttpResponse) result).getResponse();
         } else {
            return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                  "Execution of " + url
                        + " didn't return a TransportResultHttpResponse: "
                        + result.getClass().getName());
         }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
               "Execution of " + url + " threw an exception: " + e.getMessage());

      }
   }

   public default HttpResponse executePostJson(String url, String payload, TransportOption... options) {
      try {
         final TransportResult result = execute(new TransportPayloadHttp(url, payload),
               TransportOption.concat(options, TransportOptionMethod.POST_JSON))
                     .get(SonyBindingConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
         if (result instanceof TransportResultHttpResponse) {
            return ((TransportResultHttpResponse) result).getResponse();
         } else {
            return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Execution of " + payload
                  + " didn't return a TransportResultHttpResponse: " + result.getClass().getName());
         }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
               "Execution of " + payload + " threw an exception: " + e.getMessage());

      }
   }

   public default HttpResponse executePostXml(String url, String payload, TransportOption... options) {
      try {
         final TransportResult result = execute(new TransportPayloadHttp(url, payload),
               TransportOption.concat(options, TransportOptionMethod.POST_XML))
                     .get(SonyBindingConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
         if (result instanceof TransportResultHttpResponse) {
            return ((TransportResultHttpResponse) result).getResponse();
         } else {
            return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Execution of " + payload
                  + " didn't return a TransportResultHttpResponse: " + result.getClass().getName());
         }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
               "Execution of " + payload + " threw an exception: " + e.getMessage());

      }
   }

   public default ScalarWebResult execute(ScalarWebRequest payload, TransportOption... options) {
      try {
         final TransportResult result = execute(new TransportPayloadScalarWebRequest(payload), options)
               .get(SonyBindingConstants.RSP_WAIT_TIMEOUTSECONDS, TimeUnit.SECONDS);
         if (result instanceof TransportResultScalarWebResult) {
            return ((TransportResultScalarWebResult) result).getResult();
         } else {
            return new ScalarWebResult(new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                  "Execution of " + payload
                        + " didn't return a TransportResultScalarWebResult: "
                        + result.getClass().getName()));
         }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
         return new ScalarWebResult(new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
               "Execution of " + payload + " threw an exception: " + e.getMessage()));

      }
   };
}
