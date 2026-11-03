package cc.iecuhk.iems5718.s57.demo;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import cc.iecuhk.iems5718.s57.config.SessionValidationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class DemoTests {
 final DemoController controller=new DemoController();
 final MockMvc mvc=MockMvcBuilders.standaloneSetup(controller).build();
 final ObjectMapper json=new ObjectMapper();
 @org.junit.jupiter.api.BeforeEach void enableLegacyDemoForTests() { controller.enabled=true; }
 @Test void retiredDemoRejectsNewAndExistingSessions() throws Exception {
  String token=create('a');
  controller.enabled=false;
  mvc.perform(post("/api/demo/session").header("Idempotency-Key","b".repeat(64)).contentType("application/json").content("{}")).andExpect(status().isGone());
  mvc.perform(get("/api/demo/me").header("Authorization","Bearer "+token)).andExpect(status().isGone());
 }
 String create(char c)throws Exception{return json.readTree(mvc.perform(post("/api/demo/session").header("Idempotency-Key",String.valueOf(c).repeat(64)).contentType("application/json").content("{}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();}
 @Test void isolatedCheckoutResetAndRevocation() throws Exception {
  String a=create('a'),b=create('b');assertEquals(a,create('a'));
  String order="{\"cartItems\":[{\"pid\":1,\"quantity\":2}],\"vcode\":\"DEMO10\"}";
  for(int i=0;i<2;i++)mvc.perform(post("/api/demo/checkout").header("Authorization","Bearer "+a).header("Idempotency-Key","c".repeat(64)).contentType("application/json").content(order)).andExpect(status().isOk());
  mvc.perform(get("/api/demo/orders").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("SIMULATED")).andExpect(jsonPath("$[0].totalPrice").value(788));
  int id=json.readTree(mvc.perform(get("/api/demo/orders").header("Authorization","Bearer "+a)).andReturn().getResponse().getContentAsString()).get(0).get("id").asInt();
  mvc.perform(get("/api/demo/orders/"+id).header("Authorization","Bearer "+b)).andExpect(status().isNotFound());
  mvc.perform(post("/api/demo/reset").header("Authorization","Bearer "+b).contentType("application/json").content("{}")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/orders").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(post("/api/demo/exit").header("Authorization","Bearer "+a).contentType("application/json").content("{}")).andExpect(status().isOk());
  mvc.perform(get("/api/demo/me").header("Authorization","Bearer "+a)).andExpect(status().isUnauthorized());
 }
 @Test void limitsOriginAndExpiry() throws Exception {
  controller.createLimit=1;create('a');mvc.perform(post("/api/demo/session").header("Idempotency-Key","b".repeat(64)).contentType("application/json").content("{}")).andExpect(status().isTooManyRequests());
  mvc.perform(post("/api/demo/session").header("Origin","https://attacker.invalid").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  final long[] time={1000};Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.ofEpochMilli(time[0]);}};
  var shortLived=new DemoController(clock);shortLived.enabled=true;shortLived.idle=1;MockHttpServletRequest r=new MockHttpServletRequest("POST","/api/demo/session");r.setContentType("application/json");r.addHeader("Idempotency-Key","a".repeat(64));String token=(String)shortLived.create(r).get("token");time[0]+=1001;r.setMethod("GET");r.addHeader("Authorization","Bearer "+token);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->shortLived.me(r));shortLived.cleanup();
 }
 @Test void demoBearerNeverUsesNormalAccountEvenWithCookie() throws Exception {
  for(String path:new String[]{"/api/checkout/create-session","/api/admin/orders","/api/auth/me","/api/orders/my-orders"}){
   var request=new MockHttpServletRequest("GET",path);request.setRequestURI(path);request.addHeader("Authorization","Bearer demo_"+"a".repeat(64));var response=new MockHttpServletResponse();new SessionValidationFilter().doFilter(request,response,new MockFilterChain());assertEquals(403,response.getStatus());
  }
 }
}
