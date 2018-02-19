package org.folio.modfeefinetest;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.joda.time.format.DateTimeFormatter;

@RunWith(VertxUnitRunner.class)
public class RestVerticleIT {

    private static final String SUPPORTED_CONTENT_TYPE_JSON_DEF = "application/json";
    private static final String SUPPORTED_CONTENT_TYPE_TEXT_DEF = "text/plain";

    private static String postRequest = "{\"feefines\": \"librarianPOST\",\"desc\": \"basic lib feefines\"}";
    private static String putRequest = "{\"feefines\": \"librarianPUT\",\"desc\": \"basic lib feefines\"}";

    private static Vertx vertx;
    static int port;

//  private String groupID;
    @Rule
    public Timeout rule = Timeout.seconds(15);

    public static void initDatabase(TestContext context) throws SQLException {
        PostgresClient postgres = PostgresClient.getInstance(vertx);
        //postgres.dropCreateDatabase("test_mod_feefines");

        String sql = "drop schema if exists diku_mod_feefines cascade;\n"
                + "drop role if exists diku_mod_feefines;\n";
        Async async = context.async();
        PostgresClient.getInstance(vertx).runSQLFile(sql, true, result -> {
            if (result.failed()) {
                context.fail(result.cause());
            } else if (!result.result().isEmpty()) {
                context.fail("runSQLFile failed with: " + result.result().stream().collect(Collectors.joining(" ")));
            }
            async.complete();
        });
        async.await();
    }

    @BeforeClass
    public static void setup(TestContext context) throws SQLException {
        vertx = Vertx.vertx();

        initDatabase(context);

        Async async = context.async();
        port = NetworkUtils.nextFreePort();
        TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
        vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
            try {
                tenantClient.post(null, res2 -> {
                    async.complete();
                });
            } catch (Exception e) {
                context.fail(e);
            }
        });
    }

    @AfterClass
    public static void teardown(TestContext context) {
        context.async().complete();
    }

    /*  private Future<Void> getEmptyFeeFines(TestContext context) {
    Future future = Future.future();
    HttpClient client = vertx.createHttpClient();
    client.get(port, "localhost", "/feefines", res -> {
      if(res.statusCode() != 200) {
        res.bodyHandler(buf -> {
          String body = buf.toString();
          future.fail("Bad status code: " + res.statusCode() + " : " + body);
        });
      } else {
        res.bodyHandler(buf -> {
          JsonObject feefineCollectionObject = buf.toJsonObject();
          if(feefineCollectionObject.getJsonArray("feefines").size() == 0 &&
                  feefineCollectionObject.getInteger("totalRecords") == 00) {
            future.complete();
          } else {
            future.fail("Invalid return JSON: " + buf.toString());
          }
        });
      }
    })
            .putHeader("X-Okapi-Tenant", "diku")
            .putHeader("content-type", "application/json")
            .putHeader("accept", "application/json")
            .end();
    return future;
  }
     */
    private Future<Void> postFeefine(TestContext context) {
        Future future = Future.future();
        System.out.println(" postFeefine");
        System.out.println(" postFeefine");
        System.out.println(" postFeefine");

        JsonObject feefineObject = new JsonObject()
                .put("id", "1234567")
                .put("feeFineType", "por credencial")
                .put("defaultAmount", "10.00")
                .put("allowManualCreation", "false")
                .put("taxVat", "0.15")
                .put("ownerId", "Biblioteca postFeefine");
        HttpClient client = vertx.createHttpClient();
        client.post(port, "localhost", "/feefines", res -> {
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                future.complete();
            } else {
                future.fail("Got status code: " + res.statusCode());
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .end(feefineObject.encode());
        return future;
    }

    private Future<Void> getFeefine(TestContext context) {
        Future future = Future.future();
        HttpClient client = vertx.createHttpClient();
        client.get(port, "localhost", "/feefines", res -> {
            if (res.statusCode() == 200) {
                res.bodyHandler(buf -> {
                    JsonObject feefineObject = buf.toJsonObject();
                    if (feefineObject.getString("id").equals("joeblock")) {
                        DateFormat gmtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS\'Z\'");
                        Date createdDate = null;
                        try {
                            //createdDate = DatatypeConverter.parseDateTime(feefineObject.getString("createdDate")).getTime();
                            createdDate = new DateTime(feefineObject.getString("createdDate")).toDate();
                        } catch (Exception e) {
                            future.fail(e);
                            return;
                        }
                        Date now = new Date();
                        if (createdDate.before(now)) {
                            future.complete();
                        } else {
                            future.fail("Bad value for createdDate");
                        }
                    } else {
                        future.fail("Unable to read UNAM proper data from JSON return value: " + buf.toString());
                    }
                });
            } else {
                future.fail("Bad no hay UNAM UNAM UNAM response: " + res.statusCode());
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .end();
        return future;
    }

    private Future<Void> postAnotherFeefine(TestContext context) {
        Future future = Future.future();
        JsonObject feefineObject = new JsonObject()
                .put("id", "1234567")
                .put("feeFineType", "por Perdidad de libro")
                .put("defaultAmount", "10.00")
                .put("allowManualCreation", false)
                .put("taxVat", "0.15")
                .put("ownerId", "Biblioteca Central");

        HttpClient client = vertx.createHttpClient();
        client.post(port, "localhost", "/feefines", res -> {
            if (res.statusCode() == 201) {
                future.complete();
            } else {
                res.bodyHandler(body -> {
                    future.fail("Error adding new feefine: Got status code: " + res.statusCode() + ": " + body.toString());
                });
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .end(feefineObject.encode());
        return future;
    }

    private Future<Void> putFeefineGood(TestContext context) {
        Future future = Future.future();
        JsonObject feefineObject = new JsonObject()
                .put("id", "1234567")
                .put("feeFineType", "por credencial")
                .put("defaultAmount", "10.00")
                .put("allowManualCreation", false)
                .put("taxVat", "0.15")
                .put("ownerId", "Biblioteca Central");

        HttpClient client = vertx.createHttpClient();
        client.put(port, "localhost", "/feefines/2345678", res -> {
            if (res.statusCode() == 204) {
                future.complete();
            } else {
                res.bodyHandler(body -> {
                    future.fail("Error adding putting feefine (good): Got status code: " + res.statusCode() + ": " + body.toString());
                });
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "text/plain")
                .end(feefineObject.encode());
        return future;
    }

    private Future<Void> getGoodFeefine(TestContext context) {
        Future future = Future.future();
        HttpClient client = vertx.createHttpClient();
        client.get(port, "localhost", "/feefines/2345678", res -> {
            if (res.statusCode() == 200) {
                res.bodyHandler(buf -> {
                    JsonObject feefineObject = buf.toJsonObject();
                    if (feefineObject.getString("feefinename").equals("bobcircle")) {
                        Date createdDate = null;
                        Date updatedDate = null;
                        try {
                            createdDate = new DateTime(feefineObject.getString("createdDate")).toDate();
                            updatedDate = new DateTime(feefineObject.getString("updatedDate")).toDate();
                        } catch (Exception e) {
                            future.fail(e);
                            return;
                        }
                        Date now = new Date();
                        if (createdDate.before(now) && updatedDate.before(now) && createdDate.before(updatedDate)) {
                            future.complete();
                        } else {
                            future.fail("Bad value for createdDate and/or updatedDate");
                        }
                    } else {
                        future.fail("Unable to read proper data from JSON return value: " + buf.toString());
                    }
                });
            } else {
                future.fail("Bad response: " + res.statusCode());
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "application/json")
                .end();
        return future;
    }

    private Future<Void> putFeefineBadFeefinename(TestContext context) {
        Future future = Future.future();
        JsonObject feefineObject = new JsonObject()
                .put("id", "1234567")
                .put("feeFineType", "por credencial")
                .put("defaultAmount", "10.00")
                .put("allowManualCreation", false)
                .put("taxVat", "0.15")
                .put("ownerId", "Biblioteca Central");

        HttpClient client = vertx.createHttpClient();
        client.put(port, "localhost", "/feefines/2345678", res -> {
            if (res.statusCode() == 400) {
                future.complete();
            } else {
                res.bodyHandler(body -> {
                    future.fail("Error adding putting feefine (bad feefinename): Got status code: " + res.statusCode() + ": " + body.toString());
                });
            }
        })
                .putHeader("X-Okapi-Tenant", "diku")
                .putHeader("content-type", "application/json")
                .putHeader("accept", "text/plain")
                .end(feefineObject.encode());
        return future;
    }


    /*
 @Test
  public void doSequentialTests(TestContext context) {
     System.out.println("doSequentialTests"); 
     System.out.println("doSequentialTests"); 
     System.out.println("doSequentialTests"); 
     System.out.println("doSequentialTests"); 
    Async async = context.async();
    Future<Void> startFuture;
    Future<Void> f1 = Future.future();
    //getEmptyFeeFines(context).setHandler(f1.completer());
    startFuture = f1.compose(v -> {
      Future<Void> f = Future.future();
      postFeefine(context).setHandler(f.completer());
      return f;
    }).compose(v -> {
      Future<Void> f = Future.future();
      getFeefine(context).setHandler(f.completer());
      return f;
    });


    startFuture.setHandler(res -> {
      if(res.succeeded()) {
        async.complete();
      } else {
        context.fail(res.cause());
      }
    });
     System.out.println("Salida . Salida doSequentialTests"); 
  }
     */
    @Test
    public void testCrossTableQueries(TestContext context) {
        System.out.println("inicio");
        System.out.println("inicio");
        System.out.println("inicio");
        String url = "http://localhost:" + port + "/feefines?query=";
        String feefineUrl = "http://localhost:" + port + "/feefines";
        String ownerUrl = "http://localhost:" + port + "/owners";
        String accountUrl = "http://localhost:" + port + "/accounts";

        try {
            int inc = 0;
            CompletableFuture<Response> addFeefineCF = new CompletableFuture();
            String addFeefineURL = feefineUrl;
            // createFeefine crea el objeto json para la base de datos
            send(addFeefineURL, context, HttpMethod.POST, createFeefine(null, "74385ea9-e4c0-4270-baff-3524cbbb84b2", "Por credencial").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addFeefineCF));
            Response addFeefineResponse = addFeefineCF.get(5, TimeUnit.SECONDS);
            context.assertEquals(addFeefineResponse.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addFeefineResponse.body
                    + "\nStatus -POST " + addFeefineResponse.code + " time " + System.currentTimeMillis() + " for " + addFeefineURL);

            System.out.println("");

            CompletableFuture<Response> addFeefineCF2 = new CompletableFuture();

            send(addFeefineURL, context, HttpMethod.POST, createFeefine(null, "59fd88f4-06b7-494e-a607-a400e479cf66", "POr daño del libros").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addFeefineCF2));
            Response addFeefineResponse2 = addFeefineCF2.get(5, TimeUnit.SECONDS);
            context.assertEquals(addFeefineResponse2.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addFeefineResponse2.body
                    + "\nStatus -POST " + addFeefineResponse2.code + " time " + System.currentTimeMillis() + " for " + addFeefineURL);

            CompletableFuture<Response> addFeefineCF3 = new CompletableFuture();

            send(addFeefineURL, context, HttpMethod.POST, createFeefine(null, "74385ea9-e4c0-4270-baff-3524cbbb84b2", "Devolucion libros").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addFeefineCF3));
            Response addFeefineResponse3 = addFeefineCF3.get(5, TimeUnit.SECONDS);
            context.assertEquals(addFeefineResponse3.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addFeefineResponse3.body
                    + "\nStatus -POST " + addFeefineResponse3.code + " time " + System.currentTimeMillis() + " for " + addFeefineURL);

            CompletableFuture<Response> addOwnerCF = new CompletableFuture();
            String addOwnerURL = ownerUrl;

            send(addOwnerURL, context, HttpMethod.POST, createOwner(null, "Main Admin", "Main Library Administration Office").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addOwnerCF));
            Response addOwnerResponse = addOwnerCF.get(5, TimeUnit.SECONDS);
            context.assertEquals(addOwnerResponse.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addOwnerResponse.body
                    + "\nStatus -POST " + addOwnerResponse.code + " time " + System.currentTimeMillis() + " for " + addOwnerURL);

            CompletableFuture<Response> addOwnerCF2 = new CompletableFuture();

            send(addOwnerURL, context, HttpMethod.POST, createOwner(null, "Main Circ", "Main Library Circulation Desk").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addOwnerCF2));
            Response addOwnerResponse2 = addOwnerCF2.get(5, TimeUnit.SECONDS);
            context.assertEquals(addOwnerResponse2.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addOwnerResponse2.body
                    + "\nStatus -POST " + addOwnerResponse2.code + " time " + System.currentTimeMillis() + " for " + addOwnerURL);

            CompletableFuture<Response> addOwnerCF3 = new CompletableFuture();

            send(addOwnerURL, context, HttpMethod.POST, createOwner(null, "Olin Circ", "Olin Library Circulation Desk").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addOwnerCF3));
            Response addOwnerResponse3 = addOwnerCF3.get(5, TimeUnit.SECONDS);
            context.assertEquals(addOwnerResponse3.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addOwnerResponse3.body
                    + "\nStatus -POST " + addOwnerResponse3.code + " time " + System.currentTimeMillis() + " for " + addOwnerURL);

            CompletableFuture<Response> addAccountCF = new CompletableFuture();
            String addAccountURL = accountUrl;

            send(addAccountURL, context, HttpMethod.POST, createAccount(null,20.50,10.50,1,"Open","Paid Partially","Note account","0bab56e5-1ab6-4ac2-afdf-8b2df0434378","2b94c631-fca9-a892-c730-03ee529ffe27","04d830a8-810c-415c-aa50-4d09c1fee133","23fdb0bc-ab58-442a-b326-577a96204487","cf0e582b-58c3-48b1-8158-e4158211b74e").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addAccountCF));
            Response addAccountResponse = addAccountCF.get(5, TimeUnit.SECONDS);
            context.assertEquals(addAccountResponse.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addAccountResponse.body
                    + "\nStatus -POST " + addAccountResponse.code + " time " + System.currentTimeMillis() + " for " + addAccountURL);

            CompletableFuture<Response> addAccountCF2 = new CompletableFuture();

            send(addAccountURL, context, HttpMethod.POST, createAccount(null,5.50,5.50, 2,"Closed","Refunded Fully","note", "959f65f6-ce51-4984-99fa-5e11c1f5661c", "2b94c631-fca9-a892-c730-03ee529ffe27", "3215e796-ac83-4f79-8253-a3ca4cccbfb7", "23f2c8e1-bd5d-4f27-9398-a688c998808a", "d7549eed-bc92-4dac-8900-0cf3c839679a4").encode(),
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, 201, new HTTPResponseHandler(addAccountCF2));
            Response addAccountResponse2 = addAccountCF2.get(5, TimeUnit.SECONDS);
            context.assertEquals(addAccountResponse2.code, HttpURLConnection.HTTP_CREATED);
            System.out.println(addAccountResponse2.body
                    + "\nStatus -POST " + addAccountResponse2.code + " time " + System.currentTimeMillis() + " for " + addAccountURL);

            System.out.println("");
            //query on feefines and sort by groups
            String url1 = url + URLEncoder.encode("id=*", "UTF-8");
            System.out.println("UNAM  " + url1);
            //String url2 = url+URLEncoder.encode("active=* sortBy patronGroup.group/sort.ascending", "UTF-8");
            //query and sort on groups via feefines endpoint
            //String url3 = url+URLEncoder.encode("patronGroup.group=lib* sortBy patronGroup.group/sort.descending", "UTF-8");
            //query on feefines sort on feefines and groups
            //String url4 = url+URLEncoder.encode("active=* sortby patronGroup.group personal.lastName personal.firstName", "UTF-8");
            //query on feefines and groups sort by groups
            //String url5 = url+URLEncoder.encode("feefinename=jhandley2nd and patronGroup.group=lib* sortby patronGroup.group", "UTF-8");
            //query on feefines and sort by feefines
            String url6 = url + URLEncoder.encode("id=*", "UTF-8");
            System.out.println("UNAM  " + url6);
            //non existant group - should be 0 results
            //String url7 = url+URLEncoder.encode("feefinename=jhandley2nd and patronGroup.group=abc* sortby patronGroup.group", "UTF-8");

            CompletableFuture<Response> cqlCF6 = new CompletableFuture();
            CompletableFuture<Response> cqlCF1 = new CompletableFuture();

            String[] urls = new String[]{url1, url6};
            CompletableFuture<Response>[] cqlCF = new CompletableFuture[]{cqlCF1, cqlCF6};

            for (int i = 0; i < 2; i++) {
                CompletableFuture<Response> cf = cqlCF[i];
                String cqlURL = urls[i];
                System.out.println("Dentro del for  " + cqlURL);
                send(cqlURL, context, HttpMethod.GET, null, SUPPORTED_CONTENT_TYPE_JSON_DEF, 200,
                        new HTTPResponseHandler(cf));
                Response cqlResponse = cf.get(5, TimeUnit.SECONDS);
                context.assertEquals(cqlResponse.code, HttpURLConnection.HTTP_OK);
                context.assertEquals(3, cqlResponse.body.getInteger("total_records"));
            }
        } catch (Exception e) {
            context.fail(e.getMessage());
        }
    }

    private void send(String url, TestContext context, HttpMethod method, String content,
            String contentType, int errorCode, Handler<HttpClientResponse> handler) {
        HttpClient client = vertx.createHttpClient();
        HttpClientRequest request;
        System.out.println("valor en send  " + url);
        System.out.println("valor en send  " + url);
        System.out.println("valor en send  " + url);
        System.out.println("valor en send  " + url);
        if (content == null) {
            content = "";
        }
        Buffer buffer = Buffer.buffer(content);

        if (method == HttpMethod.POST) {
            request = client.postAbs(url);
        } else if (method == HttpMethod.DELETE) {
            request = client.deleteAbs(url);
        } else if (method == HttpMethod.GET) {
            request = client.getAbs(url);
        } else {
            request = client.putAbs(url);
        }
        request.exceptionHandler(error -> {
            context.fail(error.getMessage());
        })
                .handler(handler);
        request.putHeader("Authorization", "diku");
        request.putHeader("x-okapi-tenant", "diku");
        request.putHeader("Accept", "application/json,text/plain");
        request.putHeader("Content-type", contentType);
        request.end(buffer);
    }

    class HTTPResponseHandler implements Handler<HttpClientResponse> {

        CompletableFuture<Response> event;

        public HTTPResponseHandler(CompletableFuture<Response> cf) {
            event = cf;
        }

        @Override
        public void handle(HttpClientResponse hcr) {
            hcr.bodyHandler(bh -> {
                Response r = new Response();
                r.code = hcr.statusCode();
                r.body = bh.toJsonObject();
                event.complete(r);
            });
        }
    }

    class HTTPNoBodyResponseHandler implements Handler<HttpClientResponse> {

        CompletableFuture<Response> event;

        public HTTPNoBodyResponseHandler(CompletableFuture<Response> cf) {
            event = cf;
        }

        @Override
        public void handle(HttpClientResponse hcr) {
            Response r = new Response();
            r.code = hcr.statusCode();
            event.complete(r);
        }
    }

    class Response {

        int code;
        JsonObject body;
    }

    private boolean isSizeMatch(Response r, int size) {
        if (r.body.getInteger("totalRecords") == size) {
            return true;
        }
        return false;
    }

    private static JsonObject createFeefine(String id, String biblioteca, String typefeefine) {

        int inc = 0;
        JsonObject feefine = new JsonObject();
        if (id != null) {
            feefine.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            feefine.put("id", id);
        }
        feefine.put("feeFineType", typefeefine)
                .put("defaultAmount", "10.00")
                .put("allowManualCreation", "false")
                .put("taxVat", "0.15")
                .put("ownerId", biblioteca);

        return feefine;
    }

    private static JsonObject createOwner(String id, String owner, String desc) {

        int inc = 0;
        JsonObject ownerJO = new JsonObject();
        if (id != null) {
            ownerJO.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            ownerJO.put("id", id);
        }
        ownerJO.put("owner", owner)
                .put("desc", desc);
        return ownerJO;
    }

    private static JsonObject createAccount(String id, double amount, double remaining, Integer accountTransaction, String status, String paymentStatus, String notes, String loanId, String materialTypeId,String userId,String itemId, String feeFineId) {
    JsonObject account = new JsonObject();
    if (id

    
        != null) {
            account.put("id", id);
    }

    
        else {
            id = UUID.randomUUID().toString();
        account.put("id", id);
    }

    account.put (
    "amount", amount)
                .put("remaining", remaining)
                .put("accountTransaction", accountTransaction)
                .put("status", new JsonObject()
                    .put("name",status))
                .put("paymentStatus", new JsonObject()
                    .put("name",paymentStatus))
                .put("notes", "this is a note for the fines module")
                .put("loanId", loanId)
                .put("materialTypeId", materialTypeId)
                .put("userId", userId)
                .put("itemId", itemId)
                .put("feeFineId", feeFineId);
    return account ;
}
}

