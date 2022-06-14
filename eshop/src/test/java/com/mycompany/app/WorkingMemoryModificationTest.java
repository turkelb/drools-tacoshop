package com.mycompany.app;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.internal.io.ResourceFactory;

public class WorkingMemoryModificationTest {

	@Test
	public void testRules() {

		KieServices kieServices = KieServices.Factory.get();

		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		kieFileSystem.write(ResourceFactory.newClassPathResource("discountRules.drl"));
		KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
		kb.buildAll();
		KieModule kieModule = kb.getKieModule();
		KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

		KieSession kieSession = kieContainer.newKieSession();

		Customer customer = new Customer();
		customer.setName("John");
		Coupon coupon = new Coupon();
		coupon.setValidUntil(new Date(System.currentTimeMillis() - 3600)); // expired one hour ago
		Order order = new Order();
		List<OrderLine> orderLines = new ArrayList<OrderLine>();
		for (int i = 0; i <= 10; i++) {
			orderLines.add(new OrderLine());
		}
		order.setItems(orderLines);
		order.setCustomer(customer);
		Item item = new Item("Cheap item", 9.00, 8.00);

		coupon.setOrder(order); // bturkel

		kieSession.insert(customer);
		kieSession.insert(coupon);
		kieSession.insert(order);
		kieSession.insert(item);
		int firedRUles = kieSession.fireAllRules();

		assertThat(5, equalTo(firedRUles));

		// check it contains one object of type IsGoldCustomer
		Collection<?> goldCustomerObjs = kieSession.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object obj) {
				return obj instanceof IsGoldCustomer;
			}
		});
		assertThat(goldCustomerObjs, notNullValue());
		assertThat(1, equalTo(goldCustomerObjs.size()));
		IsGoldCustomer obj1 = (IsGoldCustomer) goldCustomerObjs.iterator().next();
		assertThat(obj1.getCustomer(), equalTo(customer));

		// check it contains one object of type IsLowRangeItem
		Collection<?> lowRangeItemObjs = kieSession.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object obj) {
				return obj instanceof IsLowRangeItem;
			}
		});
		assertThat(lowRangeItemObjs, notNullValue());
		assertThat(1, equalTo(lowRangeItemObjs.size()));
		IsLowRangeItem obj2 = (IsLowRangeItem) lowRangeItemObjs.iterator().next();
		assertThat(obj2.getItem(), equalTo(item));

	}
}
