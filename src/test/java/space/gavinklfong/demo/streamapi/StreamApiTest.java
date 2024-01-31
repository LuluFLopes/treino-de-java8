package space.gavinklfong.demo.streamapi;

import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import lombok.extern.slf4j.Slf4j;
import space.gavinklfong.demo.streamapi.models.Customer;
import space.gavinklfong.demo.streamapi.models.Order;
import space.gavinklfong.demo.streamapi.models.Product;
import space.gavinklfong.demo.streamapi.repos.CustomerRepo;
import space.gavinklfong.demo.streamapi.repos.OrderRepo;
import space.gavinklfong.demo.streamapi.repos.ProductRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@DataJpaTest
public class StreamApiTest {

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductRepo productRepo;

    @Test
    @DisplayName("Obtain a list of product with category = \"Books\" and price > 100")
    public void exercise1() {
        List<Product> products = productRepo.findAll().stream()
                .filter(prd -> prd.getCategory().equals("Books") && prd.getPrice() > 100)
                .toList();

        products.forEach(prd -> {
            assertTrue(prd.getPrice() > 100);
            assertEquals("Books", prd.getCategory());
        });
    }

    @Test
    @DisplayName("Obtain a list of product with category = \"Books\" and price > 100 (using Predicate chaining for filter)")
    public void exercise1a() {
        Predicate<Product> filtragemPorCategoria = prd -> prd.getCategory().equals("Books");
        Predicate<Product> filtragemPorValor = prd -> prd.getPrice() > 100;

        List<Product> products = productRepo.findAll().stream()
                .filter(filtragemPorCategoria)
                .filter(filtragemPorValor)
                .toList();

        products.forEach(prd -> {
            assertTrue(prd.getPrice() > 100);
            assertEquals("Books", prd.getCategory());
        });
    }

    @Test
    @DisplayName("Obtain a list of product with category = \"Books\" and price > 100 (using BiPredicate for filter)")
    public void exercise1b() {
        BiPredicate<Product, Product> criterioDeFiltragem = (prd1, prd2) -> prd1.getCategory().equals("Books") && prd2.getPrice() > 100;

        List<Product> products = productRepo.findAll().stream()
                .filter(prd -> criterioDeFiltragem.test(prd, prd))
                .toList();

        products.forEach(prd -> {
            assertTrue(prd.getPrice() > 100);
            assertEquals("Books", prd.getCategory());
        });
    }

    @Test
    @DisplayName("Obtain a list of order with product category = \"Baby\"")
    public void exercise2() {
        List<Order> baby = orderRepo.findAll().stream()
                .filter(
                        order -> order.getProducts().stream()
                                .anyMatch(product -> product.getCategory().equals("Baby"))

                ).toList();

        assertTrue(baby.size() > 0);
    }

    @Test
    @DisplayName("Obtain a list of product with category = “Toys” and then apply 10% discount\"")
    public void exercise3() {
        double totalPriceWithoutDiscount = productRepo.findAll().stream()
                .filter(prd -> prd.getCategory().equals("Toys"))
                .mapToDouble(Product::getPrice)
                .sum();

        List<Product> toysWithDiscount = productRepo.findAll().stream()
                .filter(prd -> prd.getCategory().equals("Toys"))
                .peek(prd -> prd.setPrice(prd.getPrice() * 0.90))
                .toList();

        assertEquals(Math.ceil(totalPriceWithoutDiscount * 0.9), Math.ceil(toysWithDiscount.stream().mapToDouble(Product::getPrice).sum()));
    }

    @Test
    @DisplayName("Obtain a list of products ordered by customer of tier 2 between 01-Feb-2021 and 01-Apr-2021")
    public void exercise4() {
        List<Product> products = orderRepo.findAll().stream()
                .filter(ord -> ord.getCustomer().getTier() == 2)
                .filter(ord -> ord.getOrderDate().isAfter(LocalDate.of(2021, 2, 1)))
                .filter(ord -> ord.getOrderDate().isBefore(LocalDate.of(2021, 4, 1)))
                .flatMap(ord -> ord.getProducts().stream())
                .distinct()
                .toList();

        assertEquals(19, products.size());
    }

    @Test
    @DisplayName("Get the 3 cheapest products of \"Books\" category")
    public void exercise5() {
        List<Product> products = productRepo.findAll().stream()
                .filter(prd -> prd.getCategory().equals("Books"))
                .sorted(Comparator.comparingDouble(Product::getPrice))
                .limit(3)
                .toList();

        assertEquals(3, products.size());
    }

    @Test
    @DisplayName("Get the 3 most recent placed order")
    public void exercise6() {
        List<Order> orders = orderRepo.findAll().stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(3)
                .toList();

        assertTrue(orders.size() > 0);
    }

    @Test
    @DisplayName("Get a list of products which was ordered on 15-Mar-2021")
    public void exercise7() {
        LocalDate dataPesquisada = LocalDate.of(2021, 3, 15);

        List<Product> estrategia1 = orderRepo.findAll().stream()
                .filter(ord -> ord.getOrderDate().equals(dataPesquisada))
                .map(Order::getProducts)
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        List<Product> estrategia2 = productRepo.findAll().stream()
                .filter(prd -> prd.getOrders().stream().anyMatch(ord -> ord.getOrderDate().equals(dataPesquisada)))
                .toList();

        assertEquals(estrategia1.size(), estrategia2.size());
    }

    @Test
    @DisplayName("Calculate the total lump of all orders placed in Feb 2021")
    public void exercise8() {
        double totalVendidoEmFevereiro = orderRepo.findAll().stream()
                .filter(ord -> ord.getOrderDate().getMonth().equals(LocalDate.of(2021, 2, 1).getMonth()))
                .flatMap(ord -> ord.getProducts().stream())
                .mapToDouble(Product::getPrice)
                .sum();

        assertEquals(11995.36, totalVendidoEmFevereiro);
    }

    @Test
    @DisplayName("Calculate the total lump of all orders placed in Feb 2021 (using reduce with BiFunction)")
    public void exercise8a() {
        BiFunction<Double, Product, Double> accumulator = (acc, product) -> acc + product.getPrice();

        long startTime = System.currentTimeMillis();
        double result = orderRepo.findAll()
                .stream()
                .filter(o -> o.getOrderDate().compareTo(LocalDate.of(2021, 2, 1)) >= 0)
                .filter(o -> o.getOrderDate().compareTo(LocalDate.of(2021, 3, 1)) < 0)
                .flatMap(o -> o.getProducts().stream())
                .reduce(0D, accumulator, Double::sum);

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 8a - execution time: %1$d ms", (endTime - startTime)));
        log.info("Total lump sum = " + result);
    }

    @Test
    @DisplayName("Calculate the average price of all orders placed on 15-Mar-2021")
    public void exercise9() {
        long startTime = System.currentTimeMillis();
        double result = orderRepo.findAll()
                .stream()
                .filter(o -> o.getOrderDate().isEqual(LocalDate.of(2021, 3, 15)))
                .flatMap(o -> o.getProducts().stream())
                .mapToDouble(Product::getPrice)
                .average().getAsDouble();

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 9 - execution time: %1$d ms", (endTime - startTime)));
        log.info("Average = " + result);
    }

    @Test
    @DisplayName("Obtain statistics summary of all products belong to \"Books\" category")
    public void exercise10() {
        long startTime = System.currentTimeMillis();
        DoubleSummaryStatistics statistics = productRepo.findAll()
                .stream()
                .filter(p -> p.getCategory().equalsIgnoreCase("Books"))
                .mapToDouble(Product::getPrice)
                .summaryStatistics();

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 10 - execution time: %1$d ms", (endTime - startTime)));
        log.info(String.format("count = %1$d, average = %2$f, max = %3$f, min = %4$f, sum = %5$f",
                statistics.getCount(), statistics.getAverage(), statistics.getMax(), statistics.getMin(), statistics.getSum()));

    }

    @Test
    @DisplayName("Obtain a mapping of order id and the order's product count")
    public void exercise11() {
        long startTime = System.currentTimeMillis();
        Map<Long, Integer> result = orderRepo.findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                Order::getId,
                                order -> order.getProducts().size())
                );

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 11 - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Obtain a data map of customer and list of orders")
    public void exercise12() {
        long startTime = System.currentTimeMillis();
        Map<Customer, List<Order>> result = orderRepo.findAll()
                .stream()
                .collect(Collectors.groupingBy(Order::getCustomer));

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 12 - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Obtain a data map of customer_id and list of order_id(s)")
    public void exercise12a() {
        long startTime = System.currentTimeMillis();
        HashMap<Long, List<Long>> result = orderRepo.findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getCustomer().getId(),
                                HashMap::new,
                                Collectors.mapping(Order::getId, Collectors.toList())));
        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 12a - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Obtain a data map with order and its total price")
    public void exercise13() {
        long startTime = System.currentTimeMillis();
        Map<Order, Double> result = orderRepo.findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                order -> order.getProducts().stream()
                                        .mapToDouble(Product::getPrice).sum())
                );

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 13 - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Obtain a data map with order and its total price (using reduce)")
    public void exercise13a() {
        long startTime = System.currentTimeMillis();
        Map<Long, Double> result = orderRepo.findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                Order::getId,
                                order -> order.getProducts().stream()
                                        .reduce(0D, (acc, product) -> acc + product.getPrice(), Double::sum)
                        ));

        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 13a - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Obtain a data map of product name by category")
    public void exercise14() {
        long startTime = System.currentTimeMillis();
        Map<String, List<String>> result = productRepo.findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Product::getCategory,
                                Collectors.mapping(Product::getName, Collectors.toList()))
                );


        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 14 - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

    @Test
    @DisplayName("Get the most expensive product per category")
    void exercise15() {
        long startTime = System.currentTimeMillis();
        Map<String, Optional<Product>> result = productRepo.findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Product::getCategory,
                                Collectors.maxBy(Comparator.comparing(Product::getPrice)))
                );
        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 15 - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
//		result.forEach((k,v) -> {
//			log.info("key=" + k + ", value=" + v.get());
//		});

    }

    @Test
    @DisplayName("Get the most expensive product (by name) per category")
    void exercise15a() {
        long startTime = System.currentTimeMillis();
        Map<String, String> result = productRepo.findAll()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Product::getCategory,
                                Collectors.collectingAndThen(
                                        Collectors.maxBy(Comparator.comparingDouble(Product::getPrice)),
                                        optionalProduct -> optionalProduct.map(Product::getName).orElse(null)
                                )
                        ));
        long endTime = System.currentTimeMillis();
        log.info(String.format("exercise 15a - execution time: %1$d ms", (endTime - startTime)));
        log.info(result.toString());
    }

}
