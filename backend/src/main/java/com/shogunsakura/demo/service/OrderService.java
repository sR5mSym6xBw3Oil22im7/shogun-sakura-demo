package com.shogunsakura.demo.service;

import com.shogunsakura.demo.dto.CreateOrderRequest;
import com.shogunsakura.demo.dto.OrderHistoryResponse;
import com.shogunsakura.demo.dto.OrderResponse;
import com.shogunsakura.demo.model.OrderReceipt;
import com.shogunsakura.demo.repository.OrderHistoryRepository;
import com.shogunsakura.demo.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

  private static final String PRODUCT_CODE = "SAKURA_SHOGUN_SET";
  private static final String PRODUCT_NAME = "SHOGUN SAKURA SET";
  private static final int UNIT_PRICE = 4800;

  private final OrderRepository orderRepository;
  private final OrderHistoryRepository orderHistoryRepository;

  public OrderService(OrderRepository orderRepository,
                      OrderHistoryRepository orderHistoryRepository) {
    this.orderRepository = orderRepository;
    this.orderHistoryRepository = orderHistoryRepository;
  }

  public OrderResponse createOrder(CreateOrderRequest request) {
    int totalAmount = UNIT_PRICE * request.quantity();
    OrderReceipt receipt = orderRepository.save(
        PRODUCT_CODE,
        PRODUCT_NAME,
        request.name().trim(),
        request.email().trim(),
        normalizeOptional(request.postalCode()),
        request.address().trim(),
        request.quantity(),
        UNIT_PRICE,
        totalAmount,
        normalizeOptional(request.note()));

    return new OrderResponse(receipt.id(), "注文を受け付けました。", receipt.createdAt());
  }

  public List<OrderHistoryResponse> getOrderHistory() {
    return orderHistoryRepository.findAll();
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
