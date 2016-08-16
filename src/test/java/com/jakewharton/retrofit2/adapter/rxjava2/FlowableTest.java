/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jakewharton.retrofit2.adapter.rxjava2;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static com.google.common.truth.Truth.assertThat;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;

public final class FlowableTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Flowable<String> body();
    @GET("/") Flowable<Response<String>> response();
    @GET("/") Flowable<Result<String>> result();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    subscriber.assertValues("Hi").assertComplete();
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    subscriber.assertFailureAndMessage(HttpException.class, "HTTP 404 Client Error");
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<String> subscriber = new TestSubscriber<>();
    service.body().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void bodyRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<String> subscriber = new TestSubscriber<>(0);
    Flowable<String> o = service.body();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoValues().assertNotTerminated();

    subscriber.request(1);
    subscriber.assertValues("Hi").assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertValueCount(1).assertComplete();
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    Response<String> response = subscriber.values().get(0);
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
    subscriber.assertComplete();
  }

  @Test public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    Response<String> response = subscriber.values().get(0);
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
    subscriber.assertComplete();
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>();
    service.response().subscribe(subscriber);
    subscriber.assertError(IOException.class);
  }

  @Test public void responseRespectsBackpressure() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Response<String>> subscriber = new TestSubscriber<>(0);
    Flowable<Response<String>> o = service.response();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoValues().assertNotTerminated();

    subscriber.request(1);
    Response<String> response = subscriber.values().get(0);
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
    subscriber.assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertValueCount(1).assertComplete();
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.values().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
    subscriber.assertComplete();
  }

  @Test public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.values().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
    subscriber.assertComplete();
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>();
    service.result().subscribe(subscriber);
    Result<String> result = subscriber.values().get(0);
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
    subscriber.assertComplete();
  }

  @Test public void resultRespectsBackpressure() throws IOException {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestSubscriber<Result<String>> subscriber = new TestSubscriber<>(0);
    Flowable<Result<String>> o = service.result();

    o.subscribe(subscriber);
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertNoValues().assertNotTerminated();

    subscriber.request(1);
    Result<String> result = subscriber.values().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
    subscriber.assertComplete();

    subscriber.request(Long.MAX_VALUE); // Subsequent requests do not trigger HTTP or notifications.
    assertThat(server.getRequestCount()).isEqualTo(1);
    subscriber.assertValueCount(1).assertComplete();
  }
}
