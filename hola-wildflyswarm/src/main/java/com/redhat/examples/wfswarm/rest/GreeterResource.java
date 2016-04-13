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
package com.redhat.examples.wfswarm.rest;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;
import io.fabric8.kubeflix.ribbon.KubernetesServerList;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import rx.Observable;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by ceposta 
 * <a href="http://christianposta.com/blog>http://christianposta.com/blog</a>.
 */
@Path("/api")
public class GreeterResource {

    @Inject
    @ConfigProperty(name = "WF_SWARM_SAYING", defaultValue = "Hola")
    private String saying;

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_HOST", defaultValue = "localhost")
    private String backendServiceHost;

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_PORT", defaultValue = "8080")
    private int backendServicePort;

    private String useKubernetesDiscovery;

    private ILoadBalancer loadBalancer;
    private IClientConfig config;


    public GreeterResource() {
        this.config = new DefaultClientConfigImpl();
        this.config.loadProperties("backend");

        this.useKubernetesDiscovery = System.getenv("USE_KUBERNETES_DISCOVERY");
        System.out.println("Value of USE_KUBERNETES_DISCOVERY: " + useKubernetesDiscovery);

        if ("true".equalsIgnoreCase(useKubernetesDiscovery)) {
            System.out.println("Using Kubernetes discovery for ribbon..." );
            loadBalancer = LoadBalancerBuilder.newBuilder()
                    .withDynamicServerList(new KubernetesServerList(config))
                    .buildDynamicServerListLoadBalancer();
        }

    }

    @Path("/greeting")
    @GET
    public String greeting() {
        String backendServiceUrl = String.format("http://%s:%d",  backendServiceHost,backendServicePort);
        System.out.println("Sending to: " + backendServiceUrl);

        Client client = ClientBuilder.newClient();
        BackendDTO backendDTO = client.target(backendServiceUrl)
                .path("api")
                .path("backend")
                .queryParam("greeting", saying)
                .request(MediaType.APPLICATION_JSON_TYPE).get(BackendDTO.class);

        return backendDTO.getGreeting() + " at host: " + backendDTO.getIp();
    }

    @Path("/greeting-hystrix")
    @GET
    public String greetingHystrix() {

        BackendCommand command = new BackendCommand(backendServiceHost, backendServicePort).withSaying(saying);
        BackendDTO backendDTO = command.execute();
        return backendDTO.getGreeting() + " at host: " + backendDTO.getIp();
    }

    @Path("/greeting-ribbon")
    @GET
    public String greetingRibbon() {
        if (loadBalancer == null) {
            System.out.println("Using a static list for ribbon");
            Server server = new Server(backendServiceHost, backendServicePort);
            loadBalancer = LoadBalancerBuilder.newBuilder()
                    .buildFixedServerListLoadBalancer(Arrays.asList(server));
        }

        BackendDTO backendDTO = LoadBalancerCommand.<BackendDTO>builder()
                .withLoadBalancer(loadBalancer)
                .build()
                .submit(new ServerOperation<BackendDTO>() {
                    @Override
                    public Observable<BackendDTO> call(Server server) {
                        String backendServiceUrl = String.format("http://%s:%d", server.getHost(), server.getPort());
                        System.out.println("Sending to: " + backendServiceUrl);

                        Client client = ClientBuilder.newClient();
                        return Observable.just(client.target(backendServiceUrl)
                                .path("api")
                                .path("backend")
                                .queryParam("greeting", saying)
                                .request(MediaType.APPLICATION_JSON_TYPE).get(BackendDTO.class));
                    }
                }).toBlocking().first();
        return backendDTO.getGreeting() + " at host: " + backendDTO.getIp();
    }
}
