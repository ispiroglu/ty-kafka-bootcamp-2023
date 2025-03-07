package com.trendyol.kafkabootcamp2023.orderservice.service;

import com.trendyol.kafkabootcamp2023.orderservice.domain.Order;
import com.trendyol.kafkabootcamp2023.orderservice.messaging.message.OrderCreatedMessage;
import com.trendyol.kafkabootcamp2023.orderservice.messaging.message.OrderDeliveredMessage;
import com.trendyol.kafkabootcamp2023.orderservice.messaging.producer.delivered.OrderDeliveredProducer;
import com.trendyol.kafkabootcamp2023.orderservice.model.OrderStatus;
import com.trendyol.kafkabootcamp2023.orderservice.model.exception.NotFoundException;
import com.trendyol.kafkabootcamp2023.orderservice.model.request.OrderCreateRequest;
import com.trendyol.kafkabootcamp2023.orderservice.model.request.OrderStatusUpdateRequest;
import com.trendyol.kafkabootcamp2023.orderservice.model.response.OrderResponse;
import com.trendyol.kafkabootcamp2023.orderservice.messaging.producer.created.OrderCreatedProducer;
import com.trendyol.kafkabootcamp2023.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OrderRepository orderRepository;
    private final OrderCreatedProducer orderCreatedProducer;
    private final OrderDeliveredProducer orderDeliveredProducer;

    public OrderService(OrderRepository orderRepository, OrderCreatedProducer orderCreatedProducer,  OrderDeliveredProducer orderDeliveredProducer) {
        this.orderRepository = orderRepository;
        this.orderCreatedProducer = orderCreatedProducer;
        this.orderDeliveredProducer = orderDeliveredProducer;
    }

    public OrderResponse createOrder(OrderCreateRequest orderCreateRequest) {
        Order order = orderRepository.save(orderCreateRequest.toOrder());

        orderCreatedProducer.produce(new OrderCreatedMessage(order));
        logger.info("OrderCreated with: {}", orderCreateRequest);

        return new OrderResponse(order);
    }


    public OrderResponse updateOrderStatus(OrderStatusUpdateRequest request) {
        var order = this.orderRepository.findById(request.getOrderId()).orElseThrow(
            NotFoundException::new);
        order.setOrderStatus(request.getOrderStatus());
        this.orderRepository.save(order);
        produceIfDelivered(order);

        return new OrderResponse(order);
    }

    private void produceIfDelivered(Order order) {
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            var message = new OrderDeliveredMessage(
                order
            );
            orderDeliveredProducer.produce(message);
        }
    }
}
