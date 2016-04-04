package communication;

import java.util.HashMap;
import java.util.LinkedList;





public class RootingTable
{
// ---------------------------------
// Attributes
// ---------------------------------
	private HashMap<Integer, LinkedList<Integer>> rootingTable;

// ---------------------------------
// Builder
// ---------------------------------
	public RootingTable()
	{
		this.rootingTable = new HashMap<Integer, LinkedList<Integer>>();
	}

// ---------------------------------
// Local method
// ---------------------------------
	/**
	 * @return a list of successor id sorted using the probability to reach the destination.<nl>
	 * Uses the local rooting table
	 */
	public LinkedList<Integer> getSortedRootToDestination(int destId, LinkedList<Integer> nextIdList)
	{
		LinkedList<Integer> res				= new LinkedList<Integer>();
		LinkedList<Integer> optimizedList	= new LinkedList<Integer>();
		LinkedList<Integer> knownRootList;

		if (nextIdList.contains(destId))					// Case: the destination is a successor
			optimizedList.add(destId);

		knownRootList = this.rootingTable.get(destId);
		if (knownRootList != null)							// Case: the destination is in the rooting table
			optimizedList.addAll(nextIdList);

		for (int nextId: nextIdList)						// Add all the other successor (with a lower probability)
		{
			if (optimizedList.contains(nextId))
					continue;
			res.add(nextId);
		}

		for (int nextId: optimizedList)
			res.addFirst(nextId);

		return res;
	}

	/**
	 * Update the rooting table by decreasing the proba to reach destId through nextId
	 */
	public void decreaseProba(int destId, int nextId)
	{
		LinkedList<Integer> sortedRoots = this.rootingTable.get(destId);

		if (sortedRoots == null)						// Case the destination is not in the rooting table
			return;

		if (!sortedRoots.contains(nextId))				// Case the next is not a known root to the destination
			return;

		int i;
		for (i=0; i<sortedRoots.size(); i++)
		{
			if (sortedRoots.get(i) == nextId)
			{
				sortedRoots.remove(i);
				break;
			}
		}

		if (i == sortedRoots.size())
			throw new RuntimeException();

		sortedRoots.addLast(nextId);
	}

	/**
	 * Update the rooting table by adding the next in the rooting list of the destination
	 */
	public void addRoot(int destId, int nextId)
	{
		LinkedList<Integer> sortedRoots = this.rootingTable.get(destId);

		if (sortedRoots == null)						// Case the destination is not in the rooting table
		{
			sortedRoots = new LinkedList<Integer>();
			this.rootingTable.put(destId, sortedRoots);
		}

		if (!sortedRoots.contains(nextId))				// Case nextId is not a known root to destId
		{
			sortedRoots.addFirst(nextId);
		}
	}
}