package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.List;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.impl.bpmn.behavior.LiferayMailActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.bpmn.parser.factory.AbstractBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.apache.commons.lang3.StringUtils;

public class CustomServiceTaskParseHandler extends ServiceTaskParseHandler {

	@Override
	protected void executeParse(BpmnParse bpmnParse, ServiceTask serviceTask) {
		super.executeParse(bpmnParse, serviceTask);
		FlowElement activity = bpmnParse.getCurrentFlowElement();
		// Email service tasks
	      if (StringUtils.isNotEmpty(serviceTask.getType())) {
	    	  if (serviceTask.getType().equalsIgnoreCase("mail")) {
	    		  ActivityBehaviorFactory abf = bpmnParse.getActivityBehaviorFactory();
	    		  if (abf instanceof AbstractBehaviorFactory) {
	    			  //createFieldDeclarations method available there
	    			  //otherwise use standard Mail behavior
	    			  List<FieldDeclaration> fieldDeclarations = ((AbstractBehaviorFactory) abf).createFieldDeclarations(serviceTask.getFieldExtensions());
	    			  serviceTask.setBehavior((LiferayMailActivityBehavior) ClassDelegate.defaultInstantiateDelegate(LiferayMailActivityBehavior.class, fieldDeclarations));
	    		  }
	    	  }
	      }
	}
}