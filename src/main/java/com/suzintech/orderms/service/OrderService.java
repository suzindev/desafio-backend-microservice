package com.suzintech.orderms.service;

import com.suzintech.orderms.controller.dto.OrderResponse;
import com.suzintech.orderms.entity.OrderEntity;
import com.suzintech.orderms.entity.OrderItem;
import com.suzintech.orderms.listener.dto.OrderCreatedEvent;
import com.suzintech.orderms.repository.OrderRepository;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    public OrderService(OrderRepository orderRepository, MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.mongoTemplate = mongoTemplate;
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

    public Page<OrderResponse> findAllByCustomerId(Long id, PageRequest pageRequest) {
        var orders = orderRepository.findAllByCustomerId(id, pageRequest);

        return orders.map(OrderResponse::fromEntity);
    }

    public BigDecimal findTotalOnOrdersByCustomerId(Long customerId) {
        var aggregations = newAggregation(
                match(Criteria.where("customerId").is(customerId)),
                group().sum("total").as("total")
        );

        var response = mongoTemplate.aggregate(aggregations, "tb_orders", Document.class);

        return new BigDecimal(response.getUniqueMappedResult().get("total").toString());
    }
}
