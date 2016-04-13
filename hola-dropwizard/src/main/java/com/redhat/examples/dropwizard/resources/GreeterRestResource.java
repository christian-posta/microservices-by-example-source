/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.examples.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;
import com.redhat.examples.dropwizard.api.BackendCommand;
import com.redhat.examples.dropwizard.api.BackendDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;

/**
 * Created by ceposta 
 */
@Path("/api")
public class GreeterRestResource {

    private String saying;
    private String backendServiceHost;
    private int backendServicePort;
    private Client client;

    public GreeterRestResource(final String saying, String host, int port, Client client) {
        this.saying = saying;
        this.backendServiceHost = host;
        this.backendServicePort = port;
        this.client = client;
    }

    @Path("/greeting")
    @GET
    @Timed
    public String greeting() {
        String backendServiceUrl = String.format("http://%s:%d",  backendServiceHost,backendServicePort);
        System.out.println("Sending to: " + backendServiceUrl);


        BackendDTO backendDTO = client.target(backendServiceUrl)
                .path("api")
                .path("backend")
                .queryParam("greeting", saying)
                .request().accept("application/json").get(BackendDTO.class);

        return backendDTO.getGreeting() + " at host: " + backendDTO.getIp();
    }

    @Path("/greeting-hystrix")
    @GET
    @Timed
    public String greetingHystrix() {
        BackendCommand command = new BackendCommand(backendServiceHost, backendServicePort).withSaying(saying);
        BackendDTO backendDTO = command.execute();
        return backendDTO.getGreeting() + " at host: " + backendDTO.getIp();
    }
}
