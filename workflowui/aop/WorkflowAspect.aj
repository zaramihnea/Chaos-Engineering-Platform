package com.example.workflowui.aop;

package com.example.workflowui.aop;

public aspect LoggingAspect {

    pointcut lifecycle():
    execution(* com.example.workflowui.ui.WorkflowManager.executeWorkflow(..));

    before(): lifecycle() {
        System.out.println("Before executing workflow...");
    }

    after(): lifecycle() {
        System.out.println("After executing workflow.");
    }
}