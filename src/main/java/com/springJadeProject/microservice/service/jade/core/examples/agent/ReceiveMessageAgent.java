package com.springJadeProject.microservice.service.jade.core.examples.agent;

import com.springJadeProject.microservice.service.jade.core.agent.AgentInterface;
import com.springJadeProject.microservice.service.jade.core.agent.AgentSpring;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 *
 * @author Alejandro Reyes
 */
//@Stateless @AgentQualifier(type=AgentType.RECEIVE_MESSAGE_AGENT)
@Service
@Qualifier("ReceiveMessageAgent")
public class ReceiveMessageAgent extends AgentSpring implements AgentInterface{

    @Override
    protected void setup() {
        super.setup();
        System.out.println ("------>ReceiveMessageAgent is on setup");
        System.out.println ("My name is " + this.getLocalName());
    }

    @Override
    protected void takeDown() {
        System.out.println("---->ReceiveMessageAgent is on takeDown: terminating");
    }

    @Override
    protected AgentInterface getNewInstance() {
        return new ReceiveMessageAgent();
    }


}
