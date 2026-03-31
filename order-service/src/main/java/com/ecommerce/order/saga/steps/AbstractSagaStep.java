package com.ecommerce.order.saga.steps;

import lombok.extern.slf4j.Slf4j;

/**
 * Template Method pattern: defines the skeleton of the saga step algorithm.
 * Subclasses provide concrete implementations for each phase.
 */
@Slf4j
public abstract class AbstractSagaStep<T> {

    public final T process(T context) {
        try {
            validate(context);
            T result = execute(context);
            onSuccess(result);
            return result;
        } catch (Exception ex) {
            log.error("Saga step {} failed: {}", getClass().getSimpleName(), ex.getMessage(), ex);
            onFailure(context, ex);
            throw ex;
        }
    }

    protected abstract void validate(T context);

    protected abstract T execute(T context);

    protected abstract void onSuccess(T context);

    protected abstract void onFailure(T context, Exception ex);
}
