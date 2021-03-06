package io.eventuate.examples.tram.sagas.ordersandcustomers.customers.service;

import io.eventuate.examples.tram.sagas.ordersandcustomers.customers.api.commands.ReserveCreditCommand;
import io.eventuate.examples.tram.sagas.ordersandcustomers.customers.api.replies.CustomerCreditReservationFailed;
import io.eventuate.examples.tram.sagas.ordersandcustomers.customers.api.replies.CustomerCreditReserved;
import io.eventuate.examples.tram.sagas.ordersandcustomers.customers.domain.Customer;
import io.eventuate.examples.tram.sagas.ordersandcustomers.customers.domain.CustomerCreditLimitExceededException;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class CustomerCommandHandler {

  @PersistenceContext
  private EntityManager entityManager;

  public CommandHandlers commandHandlerDefinitions() {
    return SagaCommandHandlersBuilder
            .fromChannel("customerService")
            .onMessage(ReserveCreditCommand.class, this::reserveCredit)
            .build();
  }

  public Message reserveCredit(CommandMessage<ReserveCreditCommand> cm) {
    ReserveCreditCommand cmd = cm.getCommand();
    long customerId = cmd.getCustomerId();
    Optional<Customer> customer = Optional.ofNullable(entityManager.find(Customer.class, customerId));
    try {
      customer
              .orElseThrow(() -> new IllegalArgumentException(String.format("customer with id %s is not found", customerId)))
              .reserveCredit(cmd.getOrderId(), cmd.getOrderTotal());
      return withSuccess(new CustomerCreditReserved());
    } catch (CustomerCreditLimitExceededException e) {
      return withFailure(new CustomerCreditReservationFailed());
    }
  }

  // withLock(Customer.class, customerId).
  // TODO @Validate to trigger validation and error reply


}
