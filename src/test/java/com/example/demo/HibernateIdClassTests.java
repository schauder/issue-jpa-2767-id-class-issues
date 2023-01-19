package com.example.demo;

import com.example.demo.IdClass.CustomerPK;
import com.example.demo.IdClass.CustomerWithIdClass;
import com.example.demo.IdClass.VipCustomerWithIdClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

@SpringBootTest
@Testcontainers
class HibernateIdClassTests {

    @Container
    static PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:9.6.12")
        .withDatabaseName("demo")
        .withUsername("postgres")
        .withPassword("password");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }


    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate txTemplate;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void idClassWithoutTransaction() {
        doStuff(entityManager);
    }

    @Test
    void idClassWithHandRolledTransaction() {

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        tx.begin();

        doStuff(em);

        tx.commit();
    }

    @Test
    void idClassWithTransaction() {
        txTemplate.execute(status -> doStuff(entityManager));
    }

    @Test
    @Transactional
    void idClassWithTransactional() {
        doStuff(entityManager);
    }

    private VipCustomerWithIdClass doStuff(EntityManager em) {
//        CustomerWithIdClass customer = new CustomerWithIdClass("a", "b");
//        customer.setVersionId(123L);
//        customer.setUnitId(456L);
//
//        customer = entityManager.merge(customer);//merge object of base class, ok
//
//        customer.setFirstName("a2");
//        customer = entityManager.merge(customer);//modify object of base class and merge again, ok

        VipCustomerWithIdClass vipCustomer = new VipCustomerWithIdClass("a", "b", "888");
        vipCustomer.setVersionId(987L);
        vipCustomer.setUnitId(654L);

        vipCustomer = em.merge(vipCustomer);//merge object of subclass, ok

        vipCustomer.setVipNumber("999");
        vipCustomer = em.merge(vipCustomer);//modify object of subclass and merge again, NOT OK
        // ↑ THIS FAILS BECAUSE OF PRIMARY KEY CONFLICT. INSERT STATEMENT WAS USED INSTEAD OF UPDATE, WHY?
        // this failure only happens when:
        // 1. base class uses IdClass for composite primary key
        // 2. saving an instance of the subclass for the second time after modification
        return vipCustomer;
    }
}
