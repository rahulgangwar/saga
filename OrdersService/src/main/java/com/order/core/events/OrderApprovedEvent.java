package com.order.core.events;

import com.order.core.model.OrderStatus;
import lombok.Value;

@Value
public class OrderApprovedEvent {

  private final String orderId;
  private final OrderStatus orderStatus = OrderStatus.APPROVED;
}
