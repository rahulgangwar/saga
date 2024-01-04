package com.product.command;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
@Data
@ToString
public class CreateProductCommand {

  @TargetAggregateIdentifier private final String productId;
  private final String title;
  private final BigDecimal price;
  private final Integer quantity;
}
