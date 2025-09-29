@Component
public class BootLog implements CommandLineRunner {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BootLog.class);
  @Override public void run(String... args) {
    org.slf4j.MDC.put("userId", "u-123");
    org.slf4j.MDC.put("requestId", java.util.UUID.randomUUID().toString());
    log.info("hello structured log");
    org.slf4j.MDC.clear();
  }
}
