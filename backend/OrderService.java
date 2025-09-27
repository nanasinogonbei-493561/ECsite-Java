import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
public class OrderService {
    public void placeOrder(String orderId, String userId, int amount) {
        // ビジネスキーはStructured Argumentsで付ける
        log.info("Order placed",
                 kv("event", "order_placed"),
                 kv("orderId", orderId),
                 kv("userId", userId),
                 kv("amount", amount));
    }

    public void failOrder(String orderId, Exception e) {
        log.error("Order failed",
                  kv("event", "order_failed"),
                  kv("orderId", orderId),
                  e);
    }
}
