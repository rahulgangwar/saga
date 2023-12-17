/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.appsdeveloperblog.estore.UsersService.query;

import com.core.model.PaymentDetails;
import com.core.model.User;
import com.core.query.FetchUserPaymentDetailsQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class UserEventsHandler {

  @QueryHandler
  public User findUserPaymentDetails(FetchUserPaymentDetailsQuery query) {

    PaymentDetails paymentDetails =
        PaymentDetails.builder()
            .cardNumber("123Card")
            .cvv("123")
            .name("SERGEY KARGOPOLOV")
            .validUntilMonth(12)
            .validUntilYear(2030)
            .build();

    User user =
        User.builder()
            .firstName("Sergey")
            .lastName("Kargopolov")
            .userId(query.getUserId())
            .paymentDetails(paymentDetails)
            .build();

    return user;
  }
}
