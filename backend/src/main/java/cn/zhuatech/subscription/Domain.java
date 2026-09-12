/* 上海如静知华信息科技有限公司 https://www.zhuatech.cn/ */
package cn.zhuatech.subscription;
import org.springframework.stereotype.Component;
import java.util.*;
import java.math.*;
import java.time.*;
import static cn.zhuatech.subscription.Model.*;
import static cn.zhuatech.subscription.Engine.*;
@Component public class Domain {
 static String text(Row r,String k){return txt(r.data(),k);}
 static List<Row> linked(Engine e,User u,String module,String field,String id){return e.all(u,module).stream().filter(x->text(x,field).equals(id)).toList();}
 public void create(Engine e,User u,String module,Map<String,Object>d){
  switch(module){
   case "plans" -> require(e.all(u,module).stream().noneMatch(x->text(x,"planCode").equalsIgnoreCase(txt(d,"planCode"))),"套餐编码重复");
   case "subscriptions" -> e.ref(u,d,"plan","plans");
   case "usage" -> {
    Row sub=e.ref(u,d,"subscription","subscriptions");require(sub.state().equals("ACTIVE"),"订阅尚未生效");
    LocalDate occurred=date(d,"occurredAt");require(!occurred.isBefore(date(sub.data(),"startsAt"))&&!occurred.isAfter(LocalDate.now()),"用量日期超出订阅有效区间");
    require(e.all(u,module).stream().noneMatch(x->text(x,"eventId").equalsIgnoreCase(txt(d,"eventId"))),"用量事件已记录");
    require(linked(e,u,"invoices","subscription",sub.id()).stream().noneMatch(x->!occurred.isBefore(date(x.data(),"periodStart"))&&!occurred.isAfter(date(x.data(),"periodEnd"))),"账期已出账，不能补记用量");
   }
  }
 }
 public void edit(Engine e,User u,Row r,Map<String,Object>d){
  if(r.module().equals("plans")){require(linked(e,u,"subscriptions","plan",r.id()).isEmpty(),"套餐已有订阅，不能改写历史价格");require(e.all(u,"plans").stream().noneMatch(x->!x.id().equals(r.id())&&text(x,"planCode").equalsIgnoreCase(txt(d,"planCode"))),"套餐编码重复");}
 }
 public String action(Engine e,User u,Row r,String action,Map<String,Object>i,Map<String,Object>d){
  switch(r.module()+"."+action){
   case "subscriptions.activate" -> {e.ref(u,d,"plan","plans");require(!date(d,"startsAt").isAfter(LocalDate.now()),"起始日期尚未到达");d.put("activatedAt",Instant.now().toString());}
   case "subscriptions.suspend","subscriptions.cancel" -> d.put("statusReason",txt(i,"reason"));
   case "subscriptions.resume" -> d.remove("statusReason");
   case "subscriptions.bill" -> {
    LocalDate start=date(i,"periodStart"),end=date(i,"periodEnd");
    require(!start.isBefore(date(d,"startsAt"))&&!end.isBefore(start)&&!end.isAfter(LocalDate.now())&&end.isBefore(start.plusMonths(1).plusDays(1)),"账期必须在订阅有效期内且不超过一个月");
    require(linked(e,u,"invoices","subscription",r.id()).stream().noneMatch(x->!end.isBefore(date(x.data(),"periodStart"))&&!start.isAfter(date(x.data(),"periodEnd"))),"账期与既有账单重叠");
    Row plan=e.ref(u,d,"plan","plans");int seats=num(d,"seats").intValueExact();
    long used=linked(e,u,"usage","subscription",r.id()).stream().filter(x->{LocalDate at=date(x.data(),"occurredAt");return !at.isBefore(start)&&!at.isAfter(end);}).mapToLong(x->num(x.data(),"units").longValueExact()).sum();
    long included=num(plan.data(),"includedUnits").longValueExact()*seats;long overage=Math.max(0,used-included);
    BigDecimal fee=num(plan.data(),"monthlyFee").multiply(BigDecimal.valueOf(seats));
    BigDecimal amount=money(fee.add(num(plan.data(),"overagePrice").multiply(BigDecimal.valueOf(overage))));
    Map<String,Object> invoice=new LinkedHashMap<>();invoice.put("subscription",r.id());invoice.put("periodStart",start.toString());invoice.put("periodEnd",end.toString());invoice.put("usedUnits",used);invoice.put("includedUnits",included);invoice.put("overageUnits",overage);invoice.put("baseFee",money(fee));invoice.put("amount",amount);invoice.put("customer",txt(d,"customer"));
    e.ledger(u,"invoices","ISSUED",invoice);d.put("lastBilledThrough",end.toString());
   }
   case "invoices.pay" -> {
    require(e.all(u,"payments").stream().noneMatch(x->text(x,"paymentRef").equalsIgnoreCase(txt(i,"paymentRef"))),"收款流水号重复");
    e.ledger(u,"payments","POSTED",Map.of("invoice",r.id(),"paymentRef",txt(i,"paymentRef"),"amount",r.data().get("amount"),"receivedBy",u.username()));
    d.put("paymentRef",txt(i,"paymentRef"));d.put("paidAt",Instant.now().toString());
   }
  }
  return null;
 }
 public Map<String,Object> metrics(Engine e,User u){BigDecimal due=e.all(u,"invoices").stream().filter(r->r.state().equals("ISSUED")).map(r->num(r.data(),"amount")).reduce(BigDecimal.ZERO,BigDecimal::add);return Map.of("有效订阅",e.all(u,"subscriptions").stream().filter(r->r.state().equals("ACTIVE")).count(),"待收账单",e.all(u,"invoices").stream().filter(r->r.state().equals("ISSUED")).count(),"待收金额",money(due));}
}
