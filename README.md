Retrofit 2 RxJava 2 Adapter
===========================

An RxJava 2 `CallAdapter.Factory` implementation for Retrofit 2.

This project will move into Retrofit proper once RxJava 2 has a stable release.



Usage
-----

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .build();
```

Available types:

 * `Observable<T>`, `Observable<Response<T>>`, and `Observable<Result<T>>` where `T` is the body type.
 * `Flowable<T>`, `Flowable<Response<T>>` and `Flowable<Result<T>>` where `T` is the body type.
 * `Single<T>`, `Single<Response<T>>`, and `Single<Result<T>>`  where `T` is the body type.
 * `Maybe<T>`, `Maybe<Response<T>>`, and `Maybe<Result<T>>`  where `T` is the body type.
 * `Completable` where response bodies are discarded.



Download
--------

Gradle:
```groovy
compile 'com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0'
```
or Maven:
```xml
<dependency>
  <groupId>com.jakewharton.retrofit</groupId>
  <artifactId>retrofit2-rxjava2-adapter</artifactId>
  <version>1.0.0</version>
</dependency>
```

Snapshot versions are available in the Sonatype 'snapshots' repository: https://oss.sonatype.org/content/repositories/snapshots/



License
-------

    Copyright 2016 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
