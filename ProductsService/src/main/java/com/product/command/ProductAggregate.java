package com.product.command;

import com.core.commands.CancelProductReservationCommand;
import com.core.commands.ReserveProductCommand;
import com.core.events.ProductReservationCancelledEvent;
import com.core.events.ProductReservedEvent;
import com.product.core.events.ProductCreatedEvent;
import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

@Log4j2
@Aggregate(snapshotTriggerDefinition = "productSnapshotTriggerDefinition")
public class ProductAggregate {

  @AggregateIdentifier private String productId;
  private String title;
  private BigDecimal price;
  private Integer quantity;

  public ProductAggregate() {}

  @CommandHandler
  public ProductAggregate(CreateProductCommand createProductCommand) {
    log.info("COMMAND HANDLER: CreateProductCommand");
    // Validate Create Product Command
    if (createProductCommand.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Price cannot be less or equal than zero");
    }

    if (createProductCommand.getTitle() == null || createProductCommand.getTitle().isBlank()) {
      throw new IllegalArgumentException("Title cannot be empty");
    }

    ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
    BeanUtils.copyProperties(createProductCommand, productCreatedEvent);
    AggregateLifecycle.apply(productCreatedEvent);
  }

  @CommandHandler
  public void handle(ReserveProductCommand reserveProductCommand) {
    log.info("COMMAND HANDLER: ReserveProductCommand");
    if (quantity < reserveProductCommand.getQuantity()) {
      throw new IllegalArgumentException("Insufficient number of items in stock");
    }

    ProductReservedEvent productReservedEvent =
        ProductReservedEvent.builder()
            .orderId(reserveProductCommand.getOrderId())
            .productId(reserveProductCommand.getProductId())
            .quantity(reserveProductCommand.getQuantity())
            .userId(reserveProductCommand.getUserId())
            .build();

    AggregateLifecycle.apply(productReservedEvent);
  }

  @CommandHandler
  public void handle(CancelProductReservationCommand cancelProductReservationCommand) {
    log.info("COMMAND HANDLER: CancelProductReservationCommand");
    ProductReservationCancelledEvent productReservationCancelledEvent =
        ProductReservationCancelledEvent.builder()
            .orderId(cancelProductReservationCommand.getOrderId())
            .productId(cancelProductReservationCommand.getProductId())
            .quantity(cancelProductReservationCommand.getQuantity())
            .reason(cancelProductReservationCommand.getReason())
            .userId(cancelProductReservationCommand.getUserId())
            .build();

    AggregateLifecycle.apply(productReservationCancelledEvent);
  }

  @EventSourcingHandler
  public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
    log.info(
        "EVENT HANDLER: ProductReservationCancelledEvent : Increasing quantity by "
            + productReservationCancelledEvent.getQuantity()
            + " End quanity : "
            + (this.quantity + productReservationCancelledEvent.getQuantity()));
    this.quantity += productReservationCancelledEvent.getQuantity();
  }

  @EventSourcingHandler
  public void on(ProductCreatedEvent productCreatedEvent) {
    log.info(
        "EVENT HANDLER: ProductCreatedEvent : Initializing product with " + productCreatedEvent);
    this.productId = productCreatedEvent.getProductId();
    this.price = productCreatedEvent.getPrice();
    this.title = productCreatedEvent.getTitle();
    this.quantity = productCreatedEvent.getQuantity();
  }

  @EventSourcingHandler
  public void on(ProductReservedEvent productReservedEvent) {
    log.info(
        "EVENT HANDLER: ProductReservedEvent : Reducing product quantity by"
            + productReservedEvent.getQuantity()
            + " End quanity : "
            + (this.quantity - productReservedEvent.getQuantity()));
    this.quantity -= productReservedEvent.getQuantity();
  }
}
