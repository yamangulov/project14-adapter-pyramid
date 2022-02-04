package org.satel.eip.project14.adapter.pyramid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class PyramidApplication {
    @Autowired
    ObjectMapper objectMapper;

    private static ConfigurableApplicationContext context;

	public static void main(String[] args) {
        context = SpringApplication.run(PyramidApplication.class, args);
	}

    @PostConstruct
    public ObjectMapper configureMapper() {
        objectMapper.findAndRegisterModules();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

}
