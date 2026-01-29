package com.ujax.infrastructure.persistence.jpa;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Aspect
@Component
@Order(2)
public class SoftDeleteFilterAspect {

	@PersistenceContext
	private EntityManager em;

	@Around("@annotation(com.ujax.infrastructure.persistence.jpa.IncludeDeleted) || " +
		"@within(com.ujax.infrastructure.persistence.jpa.IncludeDeleted)")
	public Object includeDeletedScope(ProceedingJoinPoint pjp) throws Throwable {
		Session session = em.unwrap(Session.class);

		boolean wasEnabled = session.getEnabledFilter("softDeleteFilter") != null;
		session.disableFilter("softDeleteFilter");

		try {
			return pjp.proceed();
		} finally {
			if (wasEnabled) {
				session.enableFilter("softDeleteFilter");
			}
		}
	}
}
