package com.suzintech.orderms.service;

import com.suzintech.orderms.entity.OrderEntity;
import com.suzintech.orderms.entity.OrderItem;
import com.suzintech.orderms.listener.dto.OrderCreatedEvent;
import com.suzintech.orderms.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void save(OrderCreatedEvent event) {
        var entity = new OrderEntity();
        entity.setOrderId(event.codigoPedido());
        entity.setCustomerId(event.codigoCliente());
        entity.setTotal(event.items().stream()
                .map(i -> i.preco().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ONE));
        entity.setItems(event.items().stream()
                .map(i -> new OrderItem(i.produto(), i.quantidade(), i.preco()))
                .toList());

        orderRepository.save(entity);
    }
}
