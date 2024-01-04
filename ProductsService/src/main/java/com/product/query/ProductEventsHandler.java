package com.product.query;

import com.core.events.ProductReservationCancelledEvent;
import com.core.events.ProductReservedEvent;
import com.product.core.data.ProductEntity;
import com.product.core.data.ProductsRepository;
import com.product.core.events.ProductCreatedEvent;
import lombok.extern.log4j.Log4j2;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ProcessingGroup("product-group")
public class ProductEventsHandler {

  private final ProductsRepository productsRepository;

  public ProductEventsHandler(ProductsRepository productsRepository) {
    this.productsRepository = productsRepository;
  }

  @ExceptionHandler(resultType = Exception.class)
  public void handle(Exception exception) throws Exception {
    throw exception;
  }

  @ExceptionHandler(resultType = IllegalArgumentException.class)
  public void handle(IllegalArgumentException exception) {
    // Log error message
  }

  @EventHandler
  public void on(ProductCreatedEvent event) {
    log.info("EVENT HANDLER: ProductCreatedEvent");
    ProductEntity productEntity = new ProductEntity();
    BeanUtils.copyProperties(event, productEntity);

    try {
      productsRepository.save(productEntity);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
    }
  }

  @EventHandler
  public void on(ProductReservedEvent productReservedEvent) {
    log.info("EVENT HANDLER: ProductReservedEvent");
    ProductEntity productEntity =
        productsRepository.findByProductId(productReservedEvent.getProductId());

    log.info("Current product quantity " + productEntity.getQuantity());

    productEntity.setQuantity(productEntity.getQuantity() - productReservedEvent.getQuantity());

    productsRepository.save(productEntity);

    log.info("New product quantity " + productEntity.getQuantity());

    log.info(
        "ProductReservedEvent is called for productId:"
            + productReservedEvent.getProductId()
            + " and orderId: "
            + productReservedEvent.getOrderId());
  }

  @EventHandler
  public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
    log.info("EVENT HANDLER: ProductReservationCancelledEvent");

    ProductEntity currentlyStoredProduct =
        productsRepository.findByProductId(productReservationCancelledEvent.getProductId());

    log.info("Current product quantity " + currentlyStoredProduct.getQuantity());

    int newQuantity =
        currentlyStoredProduct.getQuantity() + productReservationCancelledEvent.getQuantity();
    currentlyStoredProduct.setQuantity(newQuantity);

    productsRepository.save(currentlyStoredProduct);

    log.info("New product quantity " + currentlyStoredProduct.getQuantity());
  }

  @ResetHandler
  public void reset() {
    log.info("RESET HANDLER : Deleting all products");
    productsRepository.deleteAll();
  }
}
