package eecs_391_sepia_example;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class FirstAgent extends Agent {

	private static final long serialVersionUID = -7481143097108592969L;

	public FirstAgent(int playernum) {
		super(playernum);
				
		System.out.println("Constructed My First Agent");
	}

	public Map initialStep(StateView newstate, HistoryView statehistory) {
		return middleStep(newstate, statehistory);
	}

	public Map middleStep(StateView newstate, HistoryView statehistory) {
		// This stores the action that each unit will perform
		// if there are no changes to the current actions then this
		// map will be empty.
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		// this will return a list of all of your units
		// You will need to check each unit ID to determine the unit's type
		List<Integer> myUnitIds = newstate.getUnitIds(playernum);
		
		// These will store the Unit IDs that are peasants and townhalls respectively
		List<Integer> peasantIds = new ArrayList<Integer>();
		List<Integer> townhallIds = new ArrayList<Integer>();
		
		// This loop will examine each of our unit IDs and classify them as either
		// a Townhall or a Peasant
		for(Integer unitID : myUnitIds)
		{
			// UnitViews extract information about a specified unit id
			// from the current state. Using a unit view you can determine
			// the type of the unit with the given ID as well as other information
			// such as health and resources carried.
			UnitView unit = newstate.getUnit(unitID);
			
			// To find properties that all units of a given type share
			// access the UnitTemplateView using the `getTemplateView()`
			// method of a UnitView instance. In this case we are getting
			// the type name so that we can classify our units as Peasants and Townhalls
			String unitTypeName = unit.getTemplateView().getName();
			
			if(unitTypeName.equals("TownHall"))
				townhallIds.add(unitID);
			else if(unitTypeName.equals("Peasant"))
				peasantIds.add(unitID);
			else
				System.err.println("Unexpected Unit type: " + unitTypeName);
		}
		
		// get the amount of wood and gold you have in your Town Hall
		int currentGold = newstate.getResourceAmount(playernum, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(playernum, ResourceType.WOOD);
		
		List<Integer> goldMines = newstate.getResourceNodeIds(Type.GOLD_MINE);
		List<Integer> trees = newstate.getResourceNodeIds(Type.TREE);
		
		// Now that we know the unit types we can assign our peasants to collect resources
		for(Integer peasantID : peasantIds)
		{
			Action action = null;
			if(newstate.getUnit(peasantID).getCargoAmount() > 0)
			{
				// If the agent is carrying cargo then command it to deposit what its carrying at the townhall.
				// Here we are constructing a new TargetedAction. The first parameter is the unit being commanded.
				// The second parameter is the action type, in this case a COMPOUNDDEPOSIT. The actions starting
				// with COMPOUND are convenience actions made up of multiple move actions and another final action
				// in this case DEPOSIT. The moves are determined using A* planning to the location of the unit
				// specified by the 3rd argument of the constructor.
				action = new TargetedAction(peasantID, ActionType.COMPOUNDDEPOSIT, townhallIds.get(0));
			}
			else
			{
				// If the agent isn't carrying anything instruct it to go collect either gold or wood
				// whichever you have less of
				if(currentGold < currentWood)
				{
					action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, goldMines.get(0));
				}
				else
				{
					action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, trees.get(0));
				}
			}
			
			// Put the actions in the action map.
			// Without this step your agent will do nothing.
			actions.put(peasantID, action);
		}
		
		return actions;
	}

	public void terminalStep(StateView newstate, HistoryView statehistory) {
		System.out.println("Finsihed the episode");
	}

	public void savePlayerData(OutputStream os) {
		// this agent doesn't learn so nothing needs to be saved
	}

	public void loadPlayerData(InputStream is) {
		// this agent doesn't learn so nothing is loaded
	}

}
