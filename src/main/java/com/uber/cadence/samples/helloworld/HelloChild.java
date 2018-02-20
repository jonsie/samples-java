/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.uber.cadence.samples.helloworld;

import com.uber.cadence.client.CadenceClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static com.uber.cadence.samples.common.SampleConstants.DOMAIN;

/**
 * Hello World Cadence workflow that executes a single child workflow.
 * Requires a local instance of Cadence server running.
 */
public class HelloChild {

    private static final String TASK_LIST = "HelloChild";

    /**
     * Workflow interface has to have at least one method annotated with @WorkflowMethod.
     */
    public interface GreetingWorkflow {
        /**
         * @return greeting string
         */
        @WorkflowMethod
        String getGreeting(String name);
    }

    /**
     * Activity interface is just a POJI
     */
    public interface GreetingChild {
        @WorkflowMethod
        String composeGreeting(String greeting, String name);
    }

    /**
     * GreetingWorkflow implementation that calls GreetingsActivities#printIt.
     */
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        @Override
        public String getGreeting(String name) {
            // Workflows are stateful. So new stub must be created for each new child.
            GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);

            // This is blocking call that returns only after child is completed.
            return child.composeGreeting("Hello", name );
        }
    }

    /**
     * Child workflow implementation.
     * Workflow implementation must always be public for the Cadence to be able to create instances.
     */
    public static class GreetingChildImpl implements GreetingChild {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);

        // Start a worker that hosts both parent and child workflow implementations.
        Worker worker = new Worker(DOMAIN, TASK_LIST);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class, GreetingChildImpl.class);
        // Start listening to the workflow task list.
        worker.start();

        // Start a workflow execution. Usually it is done from another program.
        CadenceClient cadenceClient = CadenceClient.newInstance(DOMAIN);
        // Get a workflow stub using the same task list the worker uses.
        WorkflowOptions workflowOptions = new WorkflowOptions.Builder()
                .setTaskList(TASK_LIST)
                .setExecutionStartToCloseTimeoutSeconds(30)
                .build();
        GreetingWorkflow workflow = cadenceClient.newWorkflowStub(GreetingWorkflow.class,
                workflowOptions);
        // Execute a workflow waiting for it complete.
        String greeting = workflow.getGreeting("World");
        System.out.println(greeting);
        System.exit(0);
    }
}
