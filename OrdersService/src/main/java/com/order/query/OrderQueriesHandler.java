package com.order.query;

import com.order.core.data.OrderEntity;
import com.order.core.data.OrdersRepository;
import com.order.core.model.OrderSummary;
import lombok.extern.log4j.Log4j2;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class OrderQueriesHandler {

  OrdersRepository ordersRepository;

  public OrderQueriesHandler(OrdersRepository ordersRepository) {
    this.ordersRepository = ordersRepository;
  }

  @QueryHandler
  public OrderSummary findOrder(FindOrderQuery findOrderQuery) {
    log.info("QUERY HANDLER: " + findOrderQuery);
    OrderEntity orderEntity = ordersRepository.findByOrderId(findOrderQuery.getOrderId());
    return new OrderSummary(orderEntity.getOrderId(), orderEntity.getOrderStatus(), "");
  }
}
