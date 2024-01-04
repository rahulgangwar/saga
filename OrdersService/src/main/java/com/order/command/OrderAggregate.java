package com.order.command;

import com.order.core.events.OrderApprovedEvent;
import com.order.core.events.OrderCreatedEvent;
import com.order.core.events.OrderRejectedEvent;
import com.order.core.model.OrderStatus;
import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Log4j2
@Aggregate
public class OrderAggregate {

  @AggregateIdentifier private String orderId;
  private String productId;
  private String userId;
  private int quantity;
  private String addressId;
  private OrderStatus orderStatus;

  public OrderAggregate() {}

  @CommandHandler
  public OrderAggregate(CreateOrderCommand createOrderCommand) {
    log.info("COMMAND HANDLER: CreateOrderCommand");
    OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
    BeanUtils.copyProperties(createOrderCommand, orderCreatedEvent);

    AggregateLifecycle.apply(orderCreatedEvent);
  }

  @CommandHandler
  public void handle(ApproveOrderCommand approveOrderCommand) {
    log.info("COMMAND HANDLER: ApproveOrderCommand");

    // Create and publish the OrderApprovedEvent
    OrderApprovedEvent orderApprovedEvent =
            new OrderApprovedEvent(approveOrderCommand.getOrderId());

    AggregateLifecycle.apply(orderApprovedEvent);
  }

  @CommandHandler
  public void handle(RejectOrderCommand rejectOrderCommand) {
    log.info("COMMAND HANDLER: RejectOrderCommand");

    OrderRejectedEvent orderRejectedEvent =
            new OrderRejectedEvent(rejectOrderCommand.getOrderId(), rejectOrderCommand.getReason());

    AggregateLifecycle.apply(orderRejectedEvent);
  }

  @EventSourcingHandler
  public void on(OrderCreatedEvent orderCreatedEvent) {
    log.info("EVENT HANDLER: OrderCreatedEvent");
    this.orderId = orderCreatedEvent.getOrderId();
    this.productId = orderCreatedEvent.getProductId();
    this.userId = orderCreatedEvent.getUserId();
    this.addressId = orderCreatedEvent.getAddressId();
    this.quantity = orderCreatedEvent.getQuantity();
    this.orderStatus = orderCreatedEvent.getOrderStatus();
  }
  
  @EventSourcingHandler
  public void on(OrderApprovedEvent orderApprovedEvent) {
    log.info("EVENT HANDLER: OrderApprovedEvent");
    this.orderStatus = orderApprovedEvent.getOrderStatus();
  }

  @EventSourcingHandler
  public void on(OrderRejectedEvent orderRejectedEvent) {
    log.info("EVENT HANDLER: OrderRejectedEvent");
    this.orderStatus = orderRejectedEvent.getOrderStatus();
  }
}
