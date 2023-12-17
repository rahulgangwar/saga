package com.product.command;

import com.product.core.data.ProductLookupEntity;
import com.product.core.data.ProductLookupRepository;
import com.product.core.events.ProductCreatedEvent;
import lombok.extern.log4j.Log4j2;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ProcessingGroup("product-group")
public class ProductLookupEventsHandler {

  private final ProductLookupRepository productLookupRepository;

  public ProductLookupEventsHandler(ProductLookupRepository productLookupRepository) {
    this.productLookupRepository = productLookupRepository;
  }

  @EventHandler
  public void on(ProductCreatedEvent event) {
    log.info("ProductLookupEventsHandler.on : " + event);
    ProductLookupEntity productLookupEntity =
        new ProductLookupEntity(event.getProductId(), event.getTitle());

    productLookupRepository.save(productLookupEntity);
  }

  @ResetHandler
  public void reset() {
    productLookupRepository.deleteAll();
  }
}
