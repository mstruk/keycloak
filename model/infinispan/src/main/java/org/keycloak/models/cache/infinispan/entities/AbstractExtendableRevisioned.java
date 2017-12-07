/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.cache.infinispan.entities;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractExtendableRevisioned extends AbstractRevisioned {
    protected ConcurrentHashMap cachedWith = new ConcurrentHashMap();

    // 0 ... uninited, 1 ... initing, 2 ... inited
    private AtomicInteger initState = new AtomicInteger(0);

    private Object waitInitMutex = new Object();


    public AbstractExtendableRevisioned(Long revision, String id) {
        super(revision, id);
    }

    /**
     * Cache things along with this cachable object
     *
     * @return
     */
    public ConcurrentHashMap getCachedWith() {
        return cachedWith;
    }

    /**
     * Mark object as being in the middle of initialisation
     *
     * Thread that marks object for initialisation is responsible for unlocking it after initialisation is complete.
     *
     * @return true if current thread managed to lock object for initialisation
     */
    public boolean lockForInit() {
        return initState.compareAndSet(0, 1);
    }

    /**
     * Mark object as being initialised or uninitialised.
     *
     * @return true if object was being initialised, false otherwise
     */
    public boolean unlockInit() {
        try {
            return initState.compareAndSet(1, 2);
        } finally {
            synchronized(waitInitMutex) {
                waitInitMutex.notifyAll();
            }
        }
    }

    /**
     * If object is currently in the middle of initialisation, wait for it to finish, but not more then
     * a specified amount of milliseconds.
     *
     * The assumption is that current thread is not the one performing initialisation.
     *
     * @param timeoutMillis maximum wait time in millis
     * @return true if upon return the object is in initialised state, false otherwise
     */
    public boolean ensureInited(long timeoutMillis) {
        synchronized (waitInitMutex) {
            if (initState.get() == 2) {
                return true;
            }
            if (initState.get() == 0) {
                return false;
            }
            try {
                // block for a maximum of timeoutMillis
                waitInitMutex.wait(timeoutMillis);
            } catch (InterruptedException e) {
                // keep thread marked interrupted
                Thread.currentThread().interrupt();
            }
            return initState.get() == 2;
        }
    }
}
