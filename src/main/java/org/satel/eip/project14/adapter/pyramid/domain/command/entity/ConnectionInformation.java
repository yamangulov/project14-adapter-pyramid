package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ConnectionInformation {

    @JsonProperty("protocolProperties")
    private ProtocolProperties protocolProperties = ProtocolProperties.DEFAULT;

    @JsonProperty("host")
    private String host;

    @JsonProperty("port")
    private String port;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("rabbitExchange")
    private String rabbitExchange;

    @JsonProperty("rabbitQueue")
    private String rabbitQueue;

    @JsonProperty("rabbitRoutingKey")
    private String rabbitRoutingKey;

    @JsonProperty("restAbsolutePath")
    private String restAbsolutePath;

    @JsonProperty("restRequestMethod")
    private RestRequestMethod restRequestMethod = RestRequestMethod.GET;

    @JsonProperty("restQuery")
    private Map<String, String> restQuery = new HashMap<>();

    @JsonProperty("restRequestParams")
    private Map<String, String> restRequestParams = new HashMap<>();

    @JsonProperty("soapAbsolutePath")
    private String soapAbsolutePath;

    @JsonProperty("soapMethod")
    private String soapMethod;

    @JsonProperty("soapServiceName")
    private String soapServiceName;

    @JsonProperty("soapMethodNameSpace")
    private String soapMethodNameSpace;

    /*
    * Хедеры, которые ожидаются в сообщении от Адаптера в ЕИП
    * */
    @JsonProperty("expectedReplyMessageHeaders")
    private Map<String, String> expectedReplyMessageHeaders = new HashMap<>();


    /*
    * Хедеры, с которыми необходимо сделать запрос от Адаптера к ИС
    * */
    @JsonProperty("requiredRequestMessageHeaders")
    private Map<String, String> requiredRequestMessageHeaders = new HashMap<>();

    /*
    * Хедеры, которые необходимо получить в ответе ИС
    * */
    @JsonProperty("requiredResponseMessageHeaders")
    private Map<String, String> requiredResponseMessageHeaders = new HashMap<>();



}
