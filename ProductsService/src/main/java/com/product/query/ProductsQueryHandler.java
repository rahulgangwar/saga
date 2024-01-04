package com.product.query;

import com.product.core.data.ProductEntity;
import com.product.core.data.ProductsRepository;
import lombok.extern.log4j.Log4j2;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
public class ProductsQueryHandler {

  private final ProductsRepository productsRepository;

  public ProductsQueryHandler(ProductsRepository productsRepository) {
    this.productsRepository = productsRepository;
  }

  @QueryHandler
  public List<ProductRestModel> findProducts(FindProductsQuery query) {
    log.info("QUERY HANDLER: " + query);
    List<ProductEntity> storedProducts = productsRepository.findAll();
    List<ProductRestModel> productsRest = new ArrayList<>();
    for (ProductEntity productEntity : storedProducts) {
      ProductRestModel productRestModel = new ProductRestModel();
      BeanUtils.copyProperties(productEntity, productRestModel);
      productsRest.add(productRestModel);
    }
    return productsRest;
  }
}
