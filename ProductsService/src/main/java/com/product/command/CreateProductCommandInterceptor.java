package com.product.command;

import com.product.core.data.ProductLookupEntity;
import com.product.core.data.ProductLookupRepository;
import java.util.List;
import java.util.function.BiFunction;

import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class CreateProductCommandInterceptor
    implements MessageDispatchInterceptor<CommandMessage<?>> {

  private final ProductLookupRepository productLookupRepository;

  public CreateProductCommandInterceptor(ProductLookupRepository productLookupRepository) {
    this.productLookupRepository = productLookupRepository;
  }

  @Override
  public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(
      List<? extends CommandMessage<?>> messages) {

    return (index, command) -> {
      log.info("INTERCEPTOR: " + command.getPayloadType());

      if (CreateProductCommand.class.equals(command.getPayloadType())) {

        CreateProductCommand createProductCommand = (CreateProductCommand) command.getPayload();

        ProductLookupEntity productLookupEntity =
            productLookupRepository.findByProductIdOrTitle(
                createProductCommand.getProductId(), createProductCommand.getTitle());

        if (productLookupEntity != null) {
          throw new IllegalStateException(
              String.format(
                  "Product with productId %s or title %s already exist",
                  createProductCommand.getProductId(), createProductCommand.getTitle()));
        }
      }

      return command;
    };
  }
}
