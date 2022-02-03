package org.satel.eip.project14.adapter.pyramid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Slf4j
@SpringBootApplication
public class PyramidApplication {
    @Autowired
    ObjectMapper objectMapper;

    private static ConfigurableApplicationContext context;

	public static void main(String[] args) {
        context = SpringApplication.run(PyramidApplication.class, args);
	}

    public static void restart() {
        ApplicationArguments args = context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(PyramidApplication.class, args.getSourceArgs());
            log.info("applicationContext restarted");
        });

        thread.setDaemon(false);
        thread.start();
    }

    @PostConstruct
    public ObjectMapper configureMapper() {
        objectMapper.findAndRegisterModules();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

}
