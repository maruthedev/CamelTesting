package com.example.democamel.config;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CamelConfig extends RouteBuilder {

    @Value("${camel.component.telegram.authorization-token}")
    private String TeleBotAuthToken;

    @Value(".[/echo ]..*[a-zA-Z0-9].*")
    private String echoPattern;

    @Value(".[/save ]..*[a-zA-Z0-9].*")
    private String savePattern;

    @Value(".[/kafka ]..*[a-zA-Z0-9].*")
    private String kafkaPattern;

    @Override
    public void configure() throws Exception {
        from("telegram:bots?authorizationToken=" + TeleBotAuthToken)
                .choice()

                // telegram -> telegram
                .when(body().regex(echoPattern))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String msg = exchange.getIn().getBody(String.class);
                        String newMsg = (msg.length() >= 5 ? msg.substring(5) : "");
                        exchange.getIn().setBody("ECHO: " + newMsg);
                    }
                })
                .to("telegram:bots?authorizationToken=" + TeleBotAuthToken)

                // telegram -> .txt
                .when(body().regex(savePattern))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String msg = exchange.getIn().getBody(String.class);
                        String newMsg = LocalDateTime.now() + ": " + (msg.length() >= 5 ? msg.substring(5) : "") + "\r\n";
                        exchange.getIn().setBody(newMsg);
                    }
                })
                .to("file://logs?fileName=log.txt&fileExist=Append")
                .setBody().constant("Saved!")
                .to("telegram:bots?authorizationToken=" + TeleBotAuthToken)

                // telegram -> kafka
                .when(body().regex(kafkaPattern))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String msg = exchange.getIn().getBody(String.class);
                        String newMsg = (msg.length() >= 6 ? msg.substring(6) : "");
                        exchange.getIn().setBody(newMsg);
                    }
                })
                .setHeader(KafkaConstants.KEY,constant(null))
                .to("kafka:topic1?brokers=localhost:9092")
                .setBody().constant("Sent to kafka!")
                .to("telegram:bots?authorizationToken=" + TeleBotAuthToken)

                .otherwise()
                .setBody().constant("A normal message")
                .to("telegram:bots?authorizationToken=" + TeleBotAuthToken);
    }
}
