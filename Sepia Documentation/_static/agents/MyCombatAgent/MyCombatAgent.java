package eecs_391_sepia_example;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class MyCombatAgent extends Agent {
	
	private int enemyPlayerNum = 1;

	public MyCombatAgent(int playernum, String[] otherargs) {
		super(playernum);
		
		if(otherargs.length > 0)
		{
			enemyPlayerNum = new Integer(otherargs[0]);
		}
		
		System.out.println("Constructed MyCombatAgent");
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate,
			HistoryView statehistory) {
		// This stores the action that each unit will perform
		// if there are no changes to the current actions then this
		// map will be empty
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		// This is a list of all of your units
		// Refer to the resource agent example for ways of
		// differentiating between different unit types based on
		// the list of IDs
		List<Integer> myUnitIDs = newstate.getUnitIds(playernum);
		
		// This is a list of enemy units
		List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
		
		if(enemyUnitIDs.size() == 0)
		{
			// Nothing to do because there is no one left to attack
			return actions;
		}
		
		// start by commanding every single unit to attack an enemy unit
		for(Integer myUnitID : myUnitIDs)
		{
			// Command all of my units to attack the first enemy unit in the list
			actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyUnitIDs.get(0)));
		}
		
		return actions;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate,
			HistoryView statehistory) {
		// This stores the action that each unit will perform
		// if there are no changes to the current actions then this
		// map will be empty
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		// This is a list of enemy units
		List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
		
		if(enemyUnitIDs.size() == 0)
		{
			// Nothing to do because there is no one left to attack
			return actions;
		}
		
		int currentStep = newstate.getTurnNumber();
		
		// go through the action history
		for(ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep-1).values())
		{
			// if the previous action is no longer in progress (either due to failure or completion)
			// then add a new action for this unit
			if(feedback.getFeedback() != ActionFeedback.INCOMPLETE)
			{
				// attack the first enemy unit in the list
				int unitID = feedback.getAction().getUnitId();
				actions.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(0)));			
			}
		}

		return actions;
	}

	@Override
	public void terminalStep(StateView newstate, HistoryView statehistory) {
		System.out.println("Finished the episode");
	}

	@Override
	public void savePlayerData(OutputStream os) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadPlayerData(InputStream is) {
		// TODO Auto-generated method stub

	}

}
