package com.example.my_api_server.service;

import com.example.my_api_server.common.MemberFixture;
import com.example.my_api_server.common.OrderCreateFixture;
import com.example.my_api_server.common.ProductFixture;
import com.example.my_api_server.entity.Member;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderProductRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest //Spring DI를 통해 모든 빈(Bean)주입 해주는 어노테이션
@ActiveProfiles("test") //application-test.yml 값을 읽는다!
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private MemberDBRepo memberDBRepo;


    @Autowired
    private OrderProductRepo orderProductRepo;

    private List<Long> getProductIds(List<Product> products) {
        return products.stream()
                .map(Product::getId)
                .toList();
    }

    @BeforeEach
    public void setup() {
        orderProductRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        orderRepo.deleteAllInBatch();
        memberDBRepo.deleteAllInBatch();
    }

    private Member getSavedMember(String password) {
        return memberDBRepo.save(MemberFixture
                .defaultMember()
                .password(password)
                .build()
        );
    }

    private List<Product> getProducts() {
        return productRepo.saveAll(ProductFixture
                .defaultProducts()
        );
    }

    //그룹 테스트
    @Nested()
    @DisplayName("주문 생성 TC")
    class OrderCreateTest {

        @Test
        @DisplayName("주문 생성 시 DB에 저장되고 주문시간이 Null이 아니다.")
        public void createOrderPersistAndReturn() {
            //given
            List<Long> counts = List.of(1L, 1L);
            Member savedMember = getSavedMember("1234"); //멤버 저장
            List<Product> products = getProducts(); //상품 저장
            List<Long> productIds = getProductIds(products); //productId 추출 작업

            //new OrderCreateDto(memberId, productId, count, orderTime);
            OrderCreateDto createDto = OrderCreateFixture.defaultDto(savedMember.getId(), productIds, counts, LocalDateTime.now());

            //when
            OrderResponseDto retDto = orderService.createOrder(createDto, LocalDateTime.now());

            //then
            assertThat(retDto.getOrderCompletedTime()).isNotNull();
        }

        @Test
        @DisplayName("주문 생성 시 상품 개수 조회 테스트")
        public void createOrderTest() {
            //given
            List<Long> counts = List.of(1L, 1L);
            Member savedMember = getSavedMember("1234");
            List<Product> products = getProducts();
            List<Long> productIds = getProductIds(products);

            OrderCreateDto createDto = OrderCreateFixture.defaultDto(savedMember.getId(), productIds, counts, LocalDateTime.now());

            //when
            OrderResponseDto resDto = orderService.createOrder(createDto, LocalDateTime.now());

            //then
            long count = orderProductRepo.count();
            assertThat(count).isEqualTo(counts.size());

        }

    }

    @Nested()
    @DisplayName("주문과 연관된 도메인 예외 TC")
    class OrderRelatedExceptionTest {

        @Test
        @DisplayName("주문 시 회원이 존재하지 않으면 예외 발생")
        public void validateMemberWhenCreateOrder() {
            //given
            List<Long> counts = List.of(1L, 1L);
            Member savedMember = getSavedMember("1234"); //멤버 저장
            List<Product> products = getProducts(); //상품 저장
            List<Long> productIds = getProductIds(products); //productId 추출 작업

            OrderCreateDto createDto = OrderCreateFixture.defaultDto(savedMember.getId(), productIds, counts, LocalDateTime.now());

            //when
            assertThatThrownBy(() -> orderService.createOrder(createDto, LocalDateTime.now()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("회원이 존재하지 않습니다.");

        }

        @Test
        @DisplayName("주문 시 존재하지 않는 상품에 대한 예외")
        public void validateProductWhenCreateOrder() {
            //given
            List<Long> counts = List.of(1L, 1L);
            Member savedMember = getSavedMember("1234"); //멤버 저장
            List<Long> productIds = List.of(9999L, 99999L); //productId 추출 작업
            List<Product> products = getProducts(); //상품 저장


            OrderCreateDto createDto = OrderCreateFixture.defaultDto(savedMember.getId(), productIds, counts, LocalDateTime.now());

            //when
            assertThatThrownBy(() -> orderService.createOrder(createDto, LocalDateTime.now()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("존재하지 않는 상품입니다.");

        }
    }
}