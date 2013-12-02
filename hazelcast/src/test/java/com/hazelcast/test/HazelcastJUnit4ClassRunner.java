/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.test;

import com.hazelcast.test.annotation.Repeat;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Run the tests randomly and log the running test.
 */
public class HazelcastJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    static {
        final String logging = "hazelcast.logging.type";
        if (System.getProperty(logging) == null) {
            System.setProperty(logging, "log4j");
        }
        if (System.getProperty(TestEnvironment.HAZELCAST_TEST_USE_NETWORK) == null) {
            System.setProperty(TestEnvironment.HAZELCAST_TEST_USE_NETWORK, "false");
        }
        System.setProperty("hazelcast.version.check.enabled", "false");
        System.setProperty("hazelcast.mancenter.enabled", "false");
        System.setProperty("hazelcast.wait.seconds.before.join", "1");
        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        System.setProperty("java.net.preferIPv4Stack", "true");

        // randomize multicast group...
        Random rand = new Random();
        int g1 = rand.nextInt(255);
        int g2 = rand.nextInt(255);
        int g3 = rand.nextInt(255);
        System.setProperty("hazelcast.multicast.group", "224." + g1 + "." + g2 + "." + g3);
    }

    public HazelcastJUnit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = super.computeTestMethods();
        Collections.shuffle(methods);
        return methods;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        long start = System.currentTimeMillis();
        String testName = method.getMethod().getDeclaringClass().getSimpleName() + "." + method.getName();
        System.out.println("Started Running Test: " + testName);
        super.runChild(method, notifier);
        float took = (float) (System.currentTimeMillis() - start) / 1000;
        System.out.println(String.format("Finished Running Test: %s in %.3f seconds.", testName, took));
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        final Statement statement = super.methodBlock(method);
        final Repeat repeatable = method.getAnnotation(Repeat.class);
        if (repeatable == null || repeatable.value() < 2) {
            return statement;
        }
        return new TestRepeater(statement, method.getMethod(), repeatable.value());
    }

    private class TestRepeater extends Statement {

        private final Statement statement;

        private final Method testMethod;

        private final int repeat;

        public TestRepeater(Statement statement, Method testMethod, int repeat) {
            this.statement = statement;
            this.testMethod = testMethod;
            this.repeat = Math.max(1, repeat);
        }

        /**
         * Invokes the next {@link Statement statement} in the execution chain for
         * the specified repeat count.
         */
        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < repeat; i++) {
                if (repeat > 1) {
                    System.out.println(String.format("---> Repeating test [%s:%s], run count [%d]",
                            testMethod.getDeclaringClass().getCanonicalName(),
                            testMethod.getName(), i + 1));
                }
                statement.evaluate();
            }
        }
    }
}
