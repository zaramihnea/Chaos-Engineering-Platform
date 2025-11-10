package com.example.workflowui.tests;

import com.example.workflowui.WorkflowManager;
import com.example.workflowui.models.Workflow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WorkflowManagerTest {

    @Test
    public void testRegisterWorkflow() {
        WorkflowManager manager = WorkflowManager.getInstance();

        Workflow wf1 = new Workflow();
        Workflow wf2 = new Workflow();

        manager.registerWorkflow(wf1);
        manager.registerWorkflow(wf2);

        assertEquals(2, manager.getWorkflows().size());
        assertTrue(manager.getWorkflows().contains(wf1));
        assertTrue(manager.getWorkflows().contains(wf2));
    }
}

