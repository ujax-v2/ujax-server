package com.ujax.infrastructure.persistence.jpa;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Aspect
@Component
@Order(1)
public class SoftDeleteFilterEnableAspect {

	@PersistenceContext
	private EntityManager em;

	@Around(
		"(within(com.ujax.application..*) ) && " +
			"(@annotation(org.springframework.transaction.annotation.Transactional) || " +
			" @within(org.springframework.transaction.annotation.Transactional))"
	)
	public Object enableSoftDeleteFilter(ProceedingJoinPoint pjp) throws Throwable {

		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			return pjp.proceed();
		}

		Session session = em.unwrap(Session.class);

		boolean alreadyEnabled = session.getEnabledFilter("softDeleteFilter") != null;
		if (!alreadyEnabled) {
			session.enableFilter("softDeleteFilter");
		}

		try {
			return pjp.proceed();
		} finally {
			if (!alreadyEnabled) {
				session.disableFilter("softDeleteFilter");
			}
		}
	}
}
