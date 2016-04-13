package com.redhat.examples.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import com.redhat.examples.dropwizard.resources.GreeterSayingFactory;
import com.redhat.examples.dropwizard.resources.HelloSayingFactory;

public class HolaDropwizardConfiguration extends Configuration {


    private HelloSayingFactory sayingFactory;
    private GreeterSayingFactory greeterSayingFactory;

    @JsonProperty("helloapp")
    public HelloSayingFactory getSayingFactory() {
        return sayingFactory;
    }

    @JsonProperty("helloapp")
    public void setSayingFactory(HelloSayingFactory sayingFactory) {
        this.sayingFactory = sayingFactory;
    }

    @JsonProperty("greeter")
    public GreeterSayingFactory getGreeterSayingFactory() {
        return greeterSayingFactory;
    }

    @JsonProperty("greeter")
    public void setGreeterSayingFactory(GreeterSayingFactory greeterSayingFactory) {
        this.greeterSayingFactory = greeterSayingFactory;
    }
}
