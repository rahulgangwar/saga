package com.order.query;

import com.order.core.data.OrderEntity;
import com.order.core.data.OrdersRepository;
import com.order.core.events.OrderApprovedEvent;
import com.order.core.events.OrderCreatedEvent;
import com.order.core.events.OrderRejectedEvent;
import lombok.extern.log4j.Log4j2;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ProcessingGroup("order-group")
public class OrderEventsHandler {

  private final OrdersRepository ordersRepository;

  public OrderEventsHandler(OrdersRepository ordersRepository) {
    this.ordersRepository = ordersRepository;
  }

  @EventHandler
  public void on(OrderCreatedEvent event) {
    log.info("EVENT HANDLER: OrderCreatedEvent");
    OrderEntity orderEntity = new OrderEntity();
    BeanUtils.copyProperties(event, orderEntity);
    ordersRepository.save(orderEntity);
  }

  @EventHandler
  public void on(OrderApprovedEvent orderApprovedEvent) {
    log.info("EVENT HANDLER: OrderApprovedEvent");
    OrderEntity orderEntity = ordersRepository.findByOrderId(orderApprovedEvent.getOrderId());
    if (orderEntity == null) {
      // TODO: Do something about it
      return;
    }

    orderEntity.setOrderStatus(orderApprovedEvent.getOrderStatus());
    ordersRepository.save(orderEntity);
  }

  @EventHandler
  public void on(OrderRejectedEvent orderRejectedEvent) {
    log.info("EVENT HANDLER: OrderRejectedEvent");
    OrderEntity orderEntity = ordersRepository.findByOrderId(orderRejectedEvent.getOrderId());
    orderEntity.setOrderStatus(orderRejectedEvent.getOrderStatus());
    ordersRepository.save(orderEntity);
  }
}
