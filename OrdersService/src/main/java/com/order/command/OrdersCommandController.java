/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.order.command;

import com.order.core.model.OrderStatus;
import com.order.core.model.OrderSummary;
import com.order.query.FindOrderQuery;
import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/orders")
public class OrdersCommandController {

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  @Autowired
  public OrdersCommandController(CommandGateway commandGateway, QueryGateway queryGateway) {
    this.commandGateway = commandGateway;
    this.queryGateway = queryGateway;
  }

  @PostMapping
  public OrderSummary createOrder(@Valid @RequestBody CreateOrderRequestDTO createOrderRequestDTO) {
    log.info("CONTROLLER : CreateOrderRequestDTO" + createOrderRequestDTO);
    String userId = "27b95829-4f3f-4ddf-8983-151ba010e35b";
    String orderId = UUID.randomUUID().toString();

    CreateOrderCommand createOrderCommand =
        CreateOrderCommand.builder()
            .addressId(createOrderRequestDTO.getAddressId())
            .productId(createOrderRequestDTO.getProductId())
            .userId(userId)
            .quantity(createOrderRequestDTO.getQuantity())
            .orderId(orderId)
            .orderStatus(OrderStatus.CREATED)
            .build();

    SubscriptionQueryResult<OrderSummary, OrderSummary> queryResult =
        queryGateway.subscriptionQuery(
            new FindOrderQuery(orderId),
            ResponseTypes.instanceOf(OrderSummary.class),
            ResponseTypes.instanceOf(OrderSummary.class));

    try {
      commandGateway.sendAndWait(createOrderCommand);
      return queryResult.updates().blockFirst();
    } finally {
      queryResult.close();
    }
  }
}
