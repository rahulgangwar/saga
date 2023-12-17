package com.product.command;

import java.util.UUID;
import javax.validation.Valid;

import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public String createProduct(@Valid @RequestBody CreateProductRestModel createProductRestModel) {
    log.info("ProductsCommandController.createProduct : " + createProductRestModel);
    CreateProductCommand createProductCommand =
        CreateProductCommand.builder()
            .price(createProductRestModel.getPrice())
            .quantity(createProductRestModel.getQuantity())
            .title(createProductRestModel.getTitle())
            .productId(UUID.randomUUID().toString())
            .build();

    String returnValue;

    returnValue = commandGateway.sendAndWait(createProductCommand);

    //		try {
    //			returnValue = commandGateway.sendAndWait(createProductCommand);
    //		} catch (Exception ex) {
    //			returnValue = ex.getLocalizedMessage();
    //		}

    return returnValue;
  }

  //	@GetMapping
  //	public String getProduct() {
  //		return "HTTP GET Handled " + env.getProperty("local.server.port");
  //	}
  //
  //	@PutMapping
  //	public String updateProduct() {
  //		return "HTTP PUT Handled";
  //	}
  //
  //	@DeleteMapping
  //	public String deleteProduct() {
  //		return "HTTP DELETE handled";
  //	}

}
