package com.order;

import com.core.commands.CancelProductReservationCommand;
import com.core.commands.ProcessPaymentCommand;
import com.core.commands.ReserveProductCommand;
import com.core.events.PaymentProcessedEvent;
import com.core.events.ProductReservationCancelledEvent;
import com.core.events.ProductReservedEvent;
import com.core.model.User;
import com.core.query.FetchUserPaymentDetailsQuery;
import com.order.command.ApproveOrderCommand;
import com.order.command.RejectOrderCommand;
import com.order.core.events.OrderApprovedEvent;
import com.order.core.events.OrderCreatedEvent;
import com.order.core.events.OrderRejectedEvent;
import com.order.core.model.OrderSummary;
import com.order.query.FindOrderQuery;
import lombok.extern.log4j.Log4j2;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Log4j2
@Saga
public class OrderSaga {
  private final String PAYMENT_PROCESSING_TIMEOUT_DEADLINE = "payment-processing-deadline";
  @Autowired private transient CommandGateway commandGateway;
  @Autowired private transient QueryGateway queryGateway;
  @Autowired private transient DeadlineManager deadlineManager;
  @Autowired private transient QueryUpdateEmitter queryUpdateEmitter;
  private String scheduleId;

  @StartSaga
  @SagaEventHandler(associationProperty = "orderId")
  public void handle(OrderCreatedEvent orderCreatedEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : OrderCreatedEvent");
    ReserveProductCommand reserveProductCommand =
        ReserveProductCommand.builder()
            .orderId(orderCreatedEvent.getOrderId())
            .productId(orderCreatedEvent.getProductId())
            .quantity(orderCreatedEvent.getQuantity())
            .userId(orderCreatedEvent.getUserId())
            .build();

    commandGateway.send(
        reserveProductCommand,
        (commandMessage, commandResultMessage) -> {
          if (commandResultMessage.isExceptional()) {
            log.info("Product could not be reserved. Sending RejectOrderCommand");
            commandGateway.send(
                new RejectOrderCommand(
                    orderCreatedEvent.getOrderId(),
                    commandResultMessage.exceptionResult().getMessage()));
          }else {
            log.info("Product reserved confirmation received.");
          }
        });
  }

  @SagaEventHandler(associationProperty = "orderId")
  public void handle(ProductReservedEvent productReservedEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : ProductReservedEvent");

    // Process user payment
    log.info(
        "ProductReserveddEvent is called for productId: "
            + productReservedEvent.getProductId()
            + " and orderId: "
            + productReservedEvent.getOrderId());

    FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery =
        new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());

    User userPaymentDetails = null;

    try {
      userPaymentDetails =
          queryGateway
              .query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class))
              .join();
    } catch (Exception ex) {
      log.error(ex.getMessage());

      // Start compensating transaction
      cancelProductReservation(productReservedEvent, ex.getMessage());
      return;
    }

    if (userPaymentDetails == null) {
      // Start compensating transaction
      cancelProductReservation(productReservedEvent, "Could not fetch user payment details");
      return;
    }

    log.info(
        "Successfully fetched user payment details for user " + userPaymentDetails.getFirstName());

    scheduleId =
        deadlineManager.schedule(
            Duration.of(120, ChronoUnit.SECONDS),
            PAYMENT_PROCESSING_TIMEOUT_DEADLINE,
            productReservedEvent);

    ProcessPaymentCommand proccessPaymentCommand =
        ProcessPaymentCommand.builder()
            .orderId(productReservedEvent.getOrderId())
            .paymentDetails(userPaymentDetails.getPaymentDetails())
            .paymentId(UUID.randomUUID().toString())
            .build();

    String result = null;
    try {
      result = commandGateway.sendAndWait(proccessPaymentCommand);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      // Start compensating transaction
      cancelProductReservation(productReservedEvent, ex.getMessage());
      return;
    }

    if (result == null) {
      log.info("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction");
      // Start compensating transaction
      cancelProductReservation(
          productReservedEvent, "Could not proccess user payment with provided payment details");
    }
  }

  private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {

    cancelDeadline();

    CancelProductReservationCommand publishProductReservationCommand =
        CancelProductReservationCommand.builder()
            .orderId(productReservedEvent.getOrderId())
            .productId(productReservedEvent.getProductId())
            .quantity(productReservedEvent.getQuantity())
            .userId(productReservedEvent.getUserId())
            .reason(reason)
            .build();

    commandGateway.send(publishProductReservationCommand);
  }

  @SagaEventHandler(associationProperty = "orderId")
  public void handle(PaymentProcessedEvent paymentProcessedEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : PaymentProcessedEvent");
    cancelDeadline();

    // Send an ApproveOrderCommand
    ApproveOrderCommand approveOrderCommand =
        new ApproveOrderCommand(paymentProcessedEvent.getOrderId());

    commandGateway.send(approveOrderCommand);
  }

  private void cancelDeadline() {
    if (scheduleId != null) {
      deadlineManager.cancelSchedule(PAYMENT_PROCESSING_TIMEOUT_DEADLINE, scheduleId);
      scheduleId = null;
    }
  }

  @EndSaga
  @SagaEventHandler(associationProperty = "orderId")
  public void handle(OrderApprovedEvent orderApprovedEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : OrderApprovedEvent");
    log.info(
        "Order is approved. Order Saga is complete for orderId: "
            + orderApprovedEvent.getOrderId());
    // SagaLifecycle.end();
    queryUpdateEmitter.emit(
        FindOrderQuery.class,
        query -> true,
        new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getOrderStatus(), ""));
  }

  @SagaEventHandler(associationProperty = "orderId")
  public void handle(ProductReservationCancelledEvent productReservationCancelledEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : ProductReservationCancelledEvent");
    // Create and send a RejectOrderCommand
    RejectOrderCommand rejectOrderCommand =
        new RejectOrderCommand(
            productReservationCancelledEvent.getOrderId(),
            productReservationCancelledEvent.getReason());

    commandGateway.send(rejectOrderCommand);
  }

  @EndSaga
  @SagaEventHandler(associationProperty = "orderId")
  public void handle(OrderRejectedEvent orderRejectedEvent) {
    log.info("SAGA ======== >> EVENT HANDLER : OrderRejectedEvent");
    log.info("Successfully rejected order with id " + orderRejectedEvent.getOrderId());

    queryUpdateEmitter.emit(
        FindOrderQuery.class,
        query -> true,
        new OrderSummary(
            orderRejectedEvent.getOrderId(),
            orderRejectedEvent.getOrderStatus(),
            orderRejectedEvent.getReason()));
  }

  @DeadlineHandler(deadlineName = PAYMENT_PROCESSING_TIMEOUT_DEADLINE)
  public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {
    log.info("SAGA ======== >> DEADLINE HANDLER : PAYMENT_PROCESSING_TIMEOUT_DEADLINE");
    log.info(
        "Payment processing deadline took place. Sending a compensating command to cancel the product reservation");
    cancelProductReservation(productReservedEvent, "Payment timeout");
  }
}
