package com.product.command;

import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/products") // http://localhost:8080/products
public class ProductsCommandController {

  private final Environment env;
  private final CommandGateway commandGateway;

  @Autowired
  public ProductsCommandController(Environment env, CommandGateway commandGateway) {
    this.env = env;
    this.commandGateway = commandGateway;
  }

  @PostMapping
  public String createProduct(@Valid @RequestBody CreateProductRequestDTO createProductRequestDTO) {
    log.info("CONTROLLER : CreateProductRestModel" + createProductRequestDTO);
    CreateProductCommand createProductCommand =
        CreateProductCommand.builder()
            .price(createProductRequestDTO.getPrice())
            .quantity(createProductRequestDTO.getQuantity())
            .title(createProductRequestDTO.getTitle())
            .productId(UUID.randomUUID().toString())
            .build();

    return commandGateway.sendAndWait(createProductCommand);
  }
}
