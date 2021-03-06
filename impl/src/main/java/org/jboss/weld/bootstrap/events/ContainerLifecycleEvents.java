/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.bootstrap.events;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.AbstractProducerBean;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.ProducerField;
import org.jboss.weld.bean.ProducerMethod;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bootstrap.api.helpers.AbstractBootstrapService;
import org.jboss.weld.event.ExtensionObserverMethodImpl;
import org.jboss.weld.injection.attributes.FieldInjectionPointAttributes;
import org.jboss.weld.injection.attributes.ParameterInjectionPointAttributes;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.reflection.Reflections;

public class ContainerLifecycleEvents extends AbstractBootstrapService {

    private boolean everythingObserved;
    private boolean processAnnotatedTypeObserved;
    private boolean processBeanObserved;
    private boolean processBeanAttributesObserved;
    private boolean processInjectionPointObserved;
    private boolean processInjectionTargetObserved;
    private boolean processProducerObserved;
    private boolean processObserverMethodObserved;
    private final AnnotationDiscovery discovery;

    private final ContainerLifecycleEventPreloader preloader;

    public ContainerLifecycleEvents(ContainerLifecycleEventPreloader preloader, AnnotationDiscovery discovery) {
        this.preloader = preloader;
        this.discovery = discovery;
    }

    public void processObserverMethod(ObserverMethod<?> observer) {
        if (observer instanceof ExtensionObserverMethodImpl<?, ?>) {
            processObserverMethodType(observer.getObservedType());
        }
    }

    protected void processObserverMethodType(Type observedType) {
        if (everythingObserved) {
            return;
        }

        Class<?> rawType = Reflections.getRawType(observedType);
        if (Object.class.equals(rawType)) {
            this.everythingObserved = true;
            this.processAnnotatedTypeObserved = true;
            this.processBeanObserved = true;
            this.processBeanAttributesObserved = true;
            this.processInjectionPointObserved = true;
            this.processInjectionTargetObserved = true;
            this.processProducerObserved = true;
            this.processObserverMethodObserved = true;
        } else if (!processAnnotatedTypeObserved && ProcessAnnotatedType.class.isAssignableFrom(rawType)) {
            processAnnotatedTypeObserved = true;
        } else if (!processBeanObserved && ProcessBean.class.isAssignableFrom(rawType)) {
            processBeanObserved = true;
        } else if (!processBeanAttributesObserved && ProcessBeanAttributes.class.isAssignableFrom(rawType)) {
            processBeanAttributesObserved = true;
        } else if (!processObserverMethodObserved && ProcessObserverMethod.class.isAssignableFrom(rawType)) {
            processObserverMethodObserved = true;
        } else if (!processProducerObserved && ProcessProducer.class.equals(rawType)) {
            processProducerObserved = true;
        } else if (!processInjectionTargetObserved && ProcessInjectionTarget.class.equals(rawType)) {
            processInjectionTargetObserved = true;
        } else if (!processInjectionPointObserved && ProcessInjectionPoint.class.equals(rawType)) {
            processInjectionPointObserved = true;
        }
    }

    public boolean isProcessAnnotatedTypeObserved() {
        return processAnnotatedTypeObserved;
    }

    public boolean isProcessBeanObserved() {
        return processBeanObserved;
    }

    public boolean isProcessBeanAttributesObserved() {
        return processBeanAttributesObserved;
    }

    public boolean isProcessObserverMethodObserved() {
        return processObserverMethodObserved;
    }

    public boolean isProcessProducerObserved() {
        return processProducerObserved;
    }

    public boolean isProcessInjectionTargetObserved() {
        return processInjectionTargetObserved;
    }

    public boolean isProcessInjectionPointObserved() {
        return processInjectionPointObserved;
    }

    public <T> ProcessAnnotatedTypeImpl<T> fireProcessAnnotatedType(BeanManagerImpl beanManager, SlimAnnotatedType<T> annotatedType, Extension source) {
        if (isProcessAnnotatedTypeObserved()) {

            ProcessAnnotatedTypeImpl<T> event = null;
            if (source == null) {
                event = new ProcessAnnotatedTypeImpl<T>(beanManager, annotatedType, discovery);
            } else {
                event = new ProcessSyntheticAnnotatedTypeImpl<T>(beanManager, annotatedType, discovery, source);
            }
            event.fire();
            return event;
        }
        return null;
    }

    public void fireProcessBean(BeanManagerImpl beanManager, Bean<?> bean) {
        if (isProcessBeanObserved()) {
            if (bean instanceof ManagedBean<?>) {
                ProcessManagedBeanImpl.fire(beanManager, (ManagedBean<?>) bean);
            } else if (bean instanceof SessionBean<?>) {
                ProcessSessionBeanImpl.fire(beanManager, Reflections.<SessionBean<Object>> cast(bean));
            } else if (bean instanceof ProducerField<?, ?>) {
                ProcessProducerFieldImpl.fire(beanManager, (ProducerField<?, ?>) bean);
            } else if (bean instanceof ProducerMethod<?, ?>) {
                ProcessProducerMethodImpl.fire(beanManager, (ProducerMethod<?, ?>) bean);
            } else {
                ProcessBeanImpl.fire(beanManager, bean);
            }
        }
    }

    public <T> ProcessBeanAttributesImpl<T> fireProcessBeanAttributes(BeanManagerImpl beanManager, BeanAttributes<T> attributes, Annotated annotated, Type type) {
        if (isProcessBeanAttributesObserved()) {
            return ProcessBeanAttributesImpl.fire(beanManager, attributes, annotated, type);
        }
        return null;
    }

    public void fireProcessInjectionTarget(BeanManagerImpl beanManager, AbstractClassBean<?> bean) {
        if (isProcessInjectionTargetObserved()) {
            AbstractProcessInjectionTarget.fire(beanManager, bean);
        }
    }

    public <X> InjectionTarget<X> fireProcessInjectionTarget(BeanManagerImpl beanManager, AnnotatedType<X> annotatedType, InjectionTarget<X> injectionTarget) {
        if (isProcessInjectionTargetObserved()) {
            return AbstractProcessInjectionTarget.fire(beanManager, annotatedType, injectionTarget);
        }
        return injectionTarget;
    }

    public <T, X> FieldInjectionPointAttributes<T, X> fireProcessInjectionPoint(FieldInjectionPointAttributes<T, X> attributes, Class<?> declaringComponentClass,
            BeanManagerImpl manager) {
        if (isProcessInjectionPointObserved()) {
            return ProcessInjectionPointImpl.fire(attributes, declaringComponentClass, manager);
        }
        return attributes;
    }

    public <T, X> ParameterInjectionPointAttributes<T, X> fireProcessInjectionPoint(ParameterInjectionPointAttributes<T, X> injectionPointAttributes,
            Class<?> declaringComponentClass, BeanManagerImpl manager) {
        if (isProcessInjectionPointObserved()) {
            return ProcessInjectionPointImpl.fire(injectionPointAttributes, declaringComponentClass, manager);
        }
        return injectionPointAttributes;
    }

    public void fireProcessObserverMethod(BeanManagerImpl beanManager, ObserverMethod<?> observer) {
        if (isProcessObserverMethodObserved()) {
            ProcessObserverMethodImpl.fire(beanManager, observer);
        }
    }

    public void fireProcessProducer(BeanManagerImpl beanManager, AbstractProducerBean<?, ?, Member> bean) {
        if (isProcessProducerObserved()) {
            ProcessProducerImpl.fire(beanManager, bean);
        }
    }

    public void preloadProcessAnnotatedType(Class<?> type) {
        if (preloader != null && isProcessAnnotatedTypeObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessAnnotatedType.class, type);
        }
    }

    public <T extends ProcessBean<?>> void preloadProcessBean(Class<T> eventRawType, Type... typeParameters) {
        if (preloader != null && isProcessBeanObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessAnnotatedType.class, typeParameters);
        }
    }

    public void preloadProcessBeanAttributes(Type type) {
        if (preloader != null && isProcessBeanAttributesObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessBeanAttributes.class, type);
        }
    }

    public void preloadProcessInjectionPoint(Type... typeParameters) {
        if (preloader != null && isProcessInjectionPointObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessInjectionPoint.class, typeParameters);
        }
    }

    public void preloadProcessInjectionTarget(Class<?> type) {
        if (preloader != null && isProcessInjectionTargetObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessInjectionTarget.class, type);
        }
    }

    public void preloadProcessObserverMethod(Type... typeParameters) {
        if (preloader != null && isProcessObserverMethodObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessObserverMethod.class, typeParameters);
        }
    }

    public void preloadProcessProducer(Type... typeParameters) {
        if (preloader != null && isProcessProducerObserved()) {
            preloader.preloadContainerLifecycleEvent(ProcessProducer.class, typeParameters);
        }
    }

    @Override
    public void cleanupAfterBoot() {
        if (preloader != null) {
            preloader.shutdown();
        }
    }

    public boolean isPreloaderEnabled() {
        return preloader != null;
    }
}
