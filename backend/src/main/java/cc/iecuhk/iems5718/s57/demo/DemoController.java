package cc.iecuhk.iems5718.s57.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;

/** A bounded disposable workspace. No repository, payment, email or fulfilment dependencies. */
@RestController
@RequestMapping("/api/demo")
@EnableScheduling
public class DemoController {
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, List<Long>> attempts = new HashMap<>();
    private final Map<String,Object> catalog;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    @Value("${DEMO_ENABLED:true}") boolean enabled = true;
    @Value("${frontend.url:http://localhost:3000}") String origin = "http://localhost:3000";
    @Value("${DEMO_IDLE_SECONDS:3600}") long idle = 3600;
    @Value("${DEMO_MAX_SECONDS:86400}") long maximum = 86400;
    @Value("${DEMO_MAX_SESSIONS:200}") int capacity = 200;
    @Value("${DEMO_CREATE_PER_HOUR:20}") int createLimit = 20;
    @Value("${DEMO_REQUESTS_PER_MINUTE:180}") int requestLimit = 180;
    static class Session {
        String token, key; long created, touched;
        List<Map<String,Object>> cart = new ArrayList<>(), orders = new ArrayList<>();
        Map<Integer,Integer> usedStock = new HashMap<>();
        List<Long> hits = new ArrayList<>();
        Map<String,Map<String,Object>> checkouts = new HashMap<>();
        boolean couponUsed;
    }
    public DemoController() { this(Clock.systemUTC()); }
    DemoController(Clock clock) {
        this.clock = clock;
        try (var in = DemoController.class.getResourceAsStream("/demo/catalog.json")) {
            catalog = new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception e) { throw new IllegalStateException("Demo catalog unavailable", e); }
    }
    private void fail(int code,String message) { throw new ResponseStatusException(HttpStatus.valueOf(code),message); }
    private void boundary(HttpServletRequest req) {
        if (!enabled) fail(503,"Demo temporarily unavailable");
        if (idle < 1 || maximum < 1 || capacity < 1 || createLimit < 1 || requestLimit < 1) fail(503,"Invalid demo resource configuration");
        String supplied = req.getHeader("Origin");
        if (supplied != null && !origin.equals(supplied)) fail(403,"Origin rejected");
        if (!req.getMethod().equals("GET") && (req.getContentType()==null || !req.getContentType().startsWith("application/json"))) fail(415,"Use application/json");
    }
    private boolean alive(Session s,long now) { return now-s.touched < idle*1000 && now-s.created < maximum*1000; }
    @Scheduled(fixedDelayString="${DEMO_CLEANUP_MILLIS:60000}")
    public synchronized void cleanup() {
        long now=clock.millis(); sessions.values().removeIf(s -> !alive(s,now));
        attempts.values().forEach(h -> h.removeIf(t -> now-t>=3600000)); attempts.values().removeIf(List::isEmpty);
    }
    private Session session(HttpServletRequest req) {
        boundary(req); String auth=req.getHeader("Authorization");
        String token=auth!=null && auth.startsWith("Bearer demo_") ? auth.substring(7) : "";
        Session s=sessions.get(token); long now=clock.millis();
        if(s==null || !alive(s,now)) { sessions.remove(token); fail(401,"Demo expired. Return to Try Demo."); }
        s.hits.removeIf(t -> now-t>=60000);
        if(s.hits.size()>=requestLimit) fail(429,"Request limit reached. Retry in a minute.");
        s.hits.add(now); s.touched=now; return s;
    }
    @PostMapping("/session")
    public synchronized Map<String,Object> create(HttpServletRequest req) {
        boundary(req); cleanup(); String key=req.getHeader("Idempotency-Key");
        if(key==null || !key.matches("[a-f0-9]{64}")) fail(400,"Random Idempotency-Key required");
        for(Session s:sessions.values()) if(s.key.equals(key)) return Map.of("token",s.token);
        if(attempts.size()>=10000 && !attempts.containsKey(req.getRemoteAddr())) fail(429,"Creation limit reached. Retry later.");
        var hits=attempts.computeIfAbsent(req.getRemoteAddr(),k -> new ArrayList<>());
        if(hits.size()>=createLimit || attempts.size()>10000) fail(429,"Creation limit reached. Retry later.");
        hits.add(clock.millis()); if(sessions.size()>=capacity) fail(503,"Demo is busy. Retry later.");
        byte[] bytes=new byte[32]; random.nextBytes(bytes); Session s=new Session();
        s.token="demo_"+HexFormat.of().formatHex(bytes); s.key=key; s.created=s.touched=clock.millis(); sessions.put(s.token,s);
        return Map.of("token",s.token);
    }
    @GetMapping("/me") public synchronized Map<String,String> me(HttpServletRequest req) { session(req); return Map.of("email","Demo visitor","role","ROLE_DEMO"); }
    @GetMapping("/catalog") public synchronized Map<String,Object> catalog(HttpServletRequest req) {
        Session s=session(req); return Map.of("categories",catalog.get("categories"),"products",products(s));
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> products(Session s) {
        return ((List<Map<String,Object>>)catalog.get("products")).stream().map(p -> { var copy=new HashMap<>(p); int id=((Number)p.get("pid")).intValue(); copy.put("stock",((Number)p.get("stock")).intValue()-s.usedStock.getOrDefault(id,0)); return (Map<String,Object>)copy; }).toList();
    }
    @GetMapping("/products/{id}") public synchronized Map<String,Object> product(HttpServletRequest req,@PathVariable int id) { return findProduct(session(req),id); }
    private Map<String,Object> findProduct(Session s,int id) { return products(s).stream().filter(p -> ((Number)p.get("pid")).intValue()==id).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    @GetMapping("/cart") public synchronized Object cart(HttpServletRequest req) { return session(req).cart; }
    public record CartItem(int pid,int quantity) {}
    public record Checkout(List<CartItem> cartItems,String vcode) {}
    private List<Map<String,Object>> validated(Session s,List<CartItem> items) {
        if(items==null || items.size()>20) fail(400,"Use at most 20 cart lines");
        Set<Integer> ids=new HashSet<>(); List<Map<String,Object>> result=new ArrayList<>();
        for(CartItem i:items) {
            if(i==null || !ids.add(i.pid()) || i.quantity()<1 || i.quantity()>99) fail(400,"Invalid cart quantity");
            var p=findProduct(s,i.pid()); if(i.quantity()>((Number)p.get("stock")).intValue()) fail(409,"Sample stock limit reached");
            result.add(Map.of("pid",i.pid(),"quantity",i.quantity()));
        }
        return result;
    }
    @PutMapping("/cart") public synchronized Object saveCart(HttpServletRequest req,@RequestBody List<CartItem> items) { Session s=session(req); s.cart=validated(s,items); return s.cart; }
    @GetMapping("/vouchers") public synchronized Object vouchers(HttpServletRequest req) { Session s=session(req); return s.couponUsed? List.of():List.of(Map.of("code","DEMO10","discount",10)); }
    @GetMapping("/validate-voucher") public synchronized Object voucher(HttpServletRequest req,@RequestParam String vcode) { Session s=session(req); if(!vcode.equals("DEMO10") || s.couponUsed) fail(400,"Use DEMO10 once per demo reset"); return Map.of("discount",10); }
    @PostMapping("/checkout") public synchronized Object checkout(HttpServletRequest req,@RequestBody Checkout input) {
        Session s=session(req); String key=req.getHeader("Idempotency-Key");
        if(key==null || !key.matches("[a-f0-9]{64}")) fail(400,"Checkout Idempotency-Key required");
        if(s.checkouts.containsKey(key)) return s.checkouts.get(key);
        if(s.orders.size()>=10) fail(429,"Demo order limit reached. Reset to continue.");
        var cart=validated(s,input.cartItems()); if(cart.isEmpty()) fail(400,"Your bag is empty");
        boolean coupon=input.vcode()!=null && !input.vcode().isBlank();
        if(coupon && (!input.vcode().equals("DEMO10") || s.couponUsed)) fail(400,"Demo voucher unavailable");
        BigDecimal subtotal=BigDecimal.ZERO; List<Map<String,Object>> lines=new ArrayList<>();
        for(var i:cart) { int pid=(int)i.get("pid"), qty=(int)i.get("quantity"); var p=findProduct(s,pid); BigDecimal price=new BigDecimal(p.get("price").toString()); subtotal=subtotal.add(price.multiply(BigDecimal.valueOf(qty))); lines.add(Map.of("id",pid,"pid",pid,"quantity",qty,"price",price,"name",p.get("name"))); }
        BigDecimal discount=coupon?subtotal.min(BigDecimal.TEN):BigDecimal.ZERO;
        Map<String,Object> order=new HashMap<>(Map.of("id",random.nextInt(Integer.MAX_VALUE-1)+1,"userid",0,"totalPrice",subtotal.subtract(discount),"currency","HKD","merchantEmail","","status","SIMULATED","createdAt",clock.millis(),"items",lines,"discountAmount",discount,"voucherCode",coupon?"DEMO10":""));
        for(var i:cart) s.usedStock.merge((int)i.get("pid"),(int)i.get("quantity"),Integer::sum);
        s.orders.add(order); s.cart=new ArrayList<>(); if(coupon)s.couponUsed=true;
        var result=Map.<String,Object>of("url","/user/orders","simulated",true); s.checkouts.put(key,result); return result;
    }
    @GetMapping("/orders") public synchronized Object orders(HttpServletRequest req) { return session(req).orders; }
    @GetMapping("/orders/{id}") public synchronized Object order(HttpServletRequest req,@PathVariable int id) { return session(req).orders.stream().filter(o -> (int)o.get("id")==id).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    @PostMapping("/reset") public synchronized Object reset(HttpServletRequest req) { Session s=session(req); s.cart.clear(); s.orders.clear(); s.usedStock.clear(); s.checkouts.clear(); s.couponUsed=false; return Map.of("reset",true); }
    @PostMapping("/exit") public synchronized Object exit(HttpServletRequest req) { Session s=session(req); sessions.remove(s.token); return Map.of("exited",true); }
}
