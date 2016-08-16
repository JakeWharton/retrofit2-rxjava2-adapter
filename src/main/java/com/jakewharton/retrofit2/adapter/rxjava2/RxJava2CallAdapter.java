/*
 * Copyright (C) 2016 Jake Wharton
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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

final class RxJava2CallAdapter implements CallAdapter<Object> {
  private static final ObservableOperator LIFT_TO_BODY =
      new ObservableOperator<Object, Response<Object>>() {
        @Override
        public Observer<? super Response<Object>> apply(final Observer<? super Object> observer) {
          return new ToBodyObserver<>(observer);
        }
      };
  private static final Function MAP_RESPONSE_TO_RESULT =
      new Function<Response<Object>, Result<Object>>() {
        @Override public Result<Object> apply(Response<Object> response) {
          return Result.response(response);
        }
      };
  private static final Function MAP_ERROR_TO_RESULT = new Function<Throwable, Result<Object>>() {
    @Override public Result<Object> apply(Throwable throwable) {
      return Result.error(throwable);
    }
  };

  private final Type responseType;
  private final Scheduler scheduler;
  private final boolean isResult;
  private final boolean isBody;
  private final boolean isFlowable;
  private final boolean isSingle;
  private final boolean isCompletable;

  RxJava2CallAdapter(Type responseType, Scheduler scheduler, boolean isResult, boolean isBody,
      boolean isFlowable, boolean isSingle, boolean isCompletable) {
    this.responseType = responseType;
    this.scheduler = scheduler;
    this.isResult = isResult;
    this.isBody = isBody;
    this.isFlowable = isFlowable;
    this.isSingle = isSingle;
    this.isCompletable = isCompletable;
  }

  @Override public Type responseType() {
    return responseType;
  }

  @Override public <R> Object adapt(Call<R> call) {
    Observable<Response<R>> responseObservable = new CallObservable<>(call);

    Observable<?> observable;
    if (isResult) {
      observable = responseObservable.map(RxJava2CallAdapter.<R>mapResponseToResult())
          .onErrorReturn(RxJava2CallAdapter.<R>mapErrorToResult());
    } else if (isBody) {
      observable = responseObservable.lift(RxJava2CallAdapter.<R>liftToBody());
    } else {
      observable = responseObservable;
    }

    if (scheduler != null) {
      observable = observable.subscribeOn(scheduler);
    }

    if (isFlowable) {
      return observable.toFlowable(BackpressureStrategy.LATEST);
    }
    if (isSingle) {
      return observable.toSingle();
    }
    if (isCompletable) {
      return observable.toCompletable();
    }
    return observable;
  }

  private static <R> ObservableOperator<R, Response<R>> liftToBody() {
    //noinspection unchecked
    return (ObservableOperator<R, Response<R>>) LIFT_TO_BODY;
  }

  private static <R> Function<Throwable, Result<R>> mapErrorToResult() {
    //noinspection unchecked
    return (Function<Throwable, Result<R>>) MAP_ERROR_TO_RESULT;
  }

  private static <R> Function<Response<R>, Result<R>> mapResponseToResult() {
    //noinspection unchecked
    return (Function<Response<R>, Result<R>>) MAP_RESPONSE_TO_RESULT;
  }

  private static class ToBodyObserver<R> implements Observer<Response<R>> {
    private final Observer<? super R> observer;
    private boolean terminated;

    ToBodyObserver(Observer<? super R> observer) {
      this.observer = observer;
    }

    @Override public void onSubscribe(Disposable disposable) {
      observer.onSubscribe(disposable);
    }

    @Override public void onNext(Response<R> response) {
      if (response.isSuccessful()) {
        observer.onNext(response.body());
      } else {
        terminated = true;
        observer.onError(new HttpException(response));
      }
    }

    @Override public void onError(Throwable throwable) {
      if (!terminated) {
        observer.onError(throwable);
      }
    }

    @Override public void onComplete() {
      if (!terminated) {
        observer.onComplete();
      }
    }
  }
}
