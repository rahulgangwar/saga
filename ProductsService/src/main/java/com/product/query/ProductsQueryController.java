package com.product.query;

import lombok.extern.log4j.Log4j2;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/products")
public class ProductsQueryController {

  @Autowired QueryGateway queryGateway;

  @GetMapping
  public List<ProductRestModel> getProducts() {
    log.info("CONTROLLER : Get product list");
    FindProductsQuery findProductsQuery = new FindProductsQuery();
    List<ProductRestModel> products =
        queryGateway
            .query(findProductsQuery, ResponseTypes.multipleInstancesOf(ProductRestModel.class))
            .join();

    return products;
  }
}
