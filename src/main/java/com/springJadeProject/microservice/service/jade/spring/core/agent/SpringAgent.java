package com.springJadeProject.microservice.service.jade.spring.core.agent;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springJadeProject.microservice.service.jade.spring.core.manager.AgentsManager;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alejandro Reyes
 */

public abstract class SpringAgent extends Agent implements AgentInterface{
    private String nickname = "";
    private List<Behaviour> behaviourList = new ArrayList<>();
    private boolean initiated = false;

//    method to check if this agent is active or not. Useful when from Spring Controller we want
//    to restart (stop and run again) the agent and JADE Container Manager is the performance is not good enough
//    when the agent is shut down we return a new instance, and the initiated variable form the 'old' instance
//    that has been shut down is changed to false. The new one will have initiated as false too.

    public boolean isInitiated (){
        return initiated;
    }

    /** super() must be called first line in overriden setup() from subclasses **/
    @OverridingMethodsMustInvokeSuper
    @Override
    protected void setup(){
        for (Behaviour b : behaviourList){
            System.out.println("-->ATTACHING BEHAVIOUR: " + b.getBehaviourName());
            addBehaviour(b);
        }
        initiated = true;
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void takeDown(){
        //so we can check certainly if the agent has been takenDown
        initiated = false;
    }

    @Override
    public void init() {
        if (nickname.equals("")){
            init(this.getClass().getSimpleName());
        }else{
            init(nickname);
        }
    }

    @Override
    public void init(String nickname) {
        try {
            if (!isInitiated()) {
                this.nickname = nickname;
                AgentsManager.addAgentToMainContainer(nickname, this);
            }else{
                throw new JadeAgentException("ERROR: The agent you are" +
                        " trying to init is already initiated");
            }
        } catch (Exception ex) {
            throw new JadeAgentException("ERROR: exception adding the" +
                    " agent to the container " + ex.getMessage());
        }
    }

    @Override
    public AgentInterface shutDownAgent() {
        AgentInterface newInstance = this;
        if (isInitiated()) {
            AgentsManager.takeDownAgent(nickname, this);
            newInstance = getNewInstance();
            for (Behaviour b : behaviourList) {
                removeBehaviour(b);
                b.reset();
                newInstance.addBehaviourToAgent(b);
            }
            newInstance.setNickname(this.nickname);
        }
        return newInstance;
    }

    @JsonIgnore //required to avoid an infinity cycle behaviour when Jackson deserialize an SpringAgent
                //by default using get* named method makes it to consider that we have a property named AgentInterface.
                //As it would be an Agent instance that refers to itself -> we fall into a loop.
    @Override
    public Agent getAgentInstance(){
        return (Agent) this;
    }


    @Override
    public boolean addBehaviourToAgent(Behaviour behaviour){
        boolean isBehaviourAdded = false;
        if (behaviour != null && !checkBehaviourExists(behaviour)) {
            behaviourList.add(behaviour);
            //added to be able of checking who is the agent owner of the behaviour no requiring the agent to be initiated //03/06/2018
            behaviour.setAgent(this);
            System.out.println("Adding behaviour " + behaviour.getBehaviourName() + " to agent " + this.getNickname() );
            isBehaviourAdded = true;
        }
        return isBehaviourAdded;
    }

    @Override
    public boolean addBehaviourToAgentAndInit(Behaviour behaviour){
        boolean isBehaviourAdded = this.addBehaviourToAgent(behaviour);
        if (isBehaviourAdded && isInitiated()){
            addBehaviour(behaviour);
        }
        return isBehaviourAdded;
    }

    //reset the exact behaviour instance
    @Override
    public boolean resetBehaviourFromAgent(Behaviour behaviour){
        boolean isBehaviourReset = false;
        if (behaviour != null && behaviourList.contains(behaviour)){
            behaviour.reset();
            if (behaviour instanceof OneShotBehaviour){//done() method always return true and reset() don't reset it actually
                removeBehaviour(behaviour);
                addBehaviour(behaviour);
            }
            isBehaviourReset = true;
        }
        return isBehaviourReset;
    }

    @Override
    public boolean resetBehaviourFromAgent(String behaviourName){
        Behaviour behaviour = getBehaviourByName(behaviourName);
        return resetBehaviourFromAgent(behaviour);
    }

    @Override
    public boolean removeBehaviourFromAgent(Behaviour behaviour){
        boolean isBehaviourRemoved = false;
        if(behaviour != null && behaviourList.contains(behaviour)){
            this.removeBehaviour(behaviour);
            isBehaviourRemoved = true;
        }
        return isBehaviourRemoved;
    }

    @Override
    public boolean removeBehaviourFromAgent(String behaviourName){
        Behaviour behaviour = getBehaviourByName(behaviourName);
        return removeBehaviourFromAgent(behaviour);
    }

    @Override
    public boolean removeBehaviourFromAgentForever(Behaviour behaviour){
        boolean isBehaviourRemoved = removeBehaviourFromAgent(behaviour);
        if (isBehaviourRemoved){
            behaviourList.remove(behaviour);
        }
        return isBehaviourRemoved;
    }

    @Override
    public boolean removeBehaviourFromAgentForever(String behaviourName){
        Behaviour behaviour = getBehaviourByName(behaviourName);
        return removeBehaviourFromAgentForever(behaviour);
    }

    @JsonIgnore
    @Override
    public List<Behaviour> getBehavioursFromAgent(){
        return behaviourList;
    }

    @Override
    public void setNickname(String nicknameIn){
        nickname = nicknameIn;
    }

    @Override
    public String getNickname(){
        String res = getAgentClassName();
        if (!nickname.equals("")){
            res = nickname;
        }
        return res;
    }

    //check if an instance with the same behaviourName exists, not the exact instance
    private boolean checkBehaviourExists(Behaviour behaviour) {
        boolean behaviourExists = false;
        if (behaviour != null) {
            behaviourExists = checkBehaviourExistsByName(behaviour.getBehaviourName());
        }
        return behaviourExists;
    }


    private boolean checkBehaviourExistsByName(String behaviourName){
        boolean behaviourExists = false;
        int index = 0;
        String behaviourNameInList;

        if (behaviourName != null){
            while (!behaviourExists && index < behaviourList.size()){
                behaviourNameInList = behaviourList.get(index).getBehaviourName();
                if (behaviourNameInList != null && behaviourName.equals(behaviourNameInList)){
                    behaviourExists = true;
                }
                index++;
            }
        }
        return behaviourExists;
    }


    private Behaviour getBehaviourByName(String behaviourName){
        Behaviour behaviourFound = null;
        int index = 0;
        String behaviourNameInList;

        if (behaviourName != null){
            while (behaviourFound == null && index < behaviourList.size()){
                behaviourNameInList = behaviourList.get(index).getBehaviourName();
                if (behaviourNameInList != null && behaviourName.equals(behaviourNameInList)){
                    behaviourFound = behaviourList.get(index);
                }
                index++;
            }
        }
        return behaviourFound;
    }

    /** ChangeStateTo INITIATED from DELETED it's not possible, so if we want to use this
     * agent again it already have DELETED (4) as state. Won't be initiated when adding it
     * to mainContainer (will be in the container but having deleted state, setup method (from superclass Agent)
     * won't be called and the Agent won't be operated to execute any behaviour...
     * we need the setup for register in yellow pages and initiate all attributes, for example
     *
     * The  best approach at the moment is just to return a new instance of the same type of agent after
     * it is shut down
     **/


    /**
     *
     * @return new instance from current class
     */
    @JsonIgnore
    public abstract AgentInterface getNewInstance();


}

