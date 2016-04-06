package test;

import java.io.File;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Scanner;

import communication.CommunicationChanel;

import p2p.EntryThread;
import p2p.Node;





public class Topology
{
// ------------------------------------------
// Attributes
// ------------------------------------------
	private Node[]	topology;

// ------------------------------------------
// Builder
// ------------------------------------------
	public Topology(String neighborhoodFile, String communicationChanelType)
	{
		LinkedList<Integer>[] neighborhood = parseTopologyFile(neighborhoodFile);
		LinkedList<CommunicationChanel> chanelList = new LinkedList<CommunicationChanel>();
		int nbrNode = neighborhood.length/2;
		this.topology = new Node[nbrNode];

		for (int i=0; i<nbrNode; i++)									// Create the nodes
		{
			this.topology[i] = new Node(i, communicationChanelType);
		}

		for (int i=0; i<nbrNode; i++)									// Create the nodes
		{
			CommunicationChanel chanel = EntryThread.connectToNode(false, communicationChanelType, i, i, "localHost");
			chanelList.addLast(chanel);
			if (chanel == null)
				throw new RuntimeException("Failed to communicate with node " + i);
			for (int nextId: neighborhood[i])
			{
				LinkedList<Object> arguments = new LinkedList<Object>();
				arguments.add(nextId);
				arguments.add("localhost");
				Object res = EntryThread.sendActionRequestToNode(chanel, i, Node.MSG_TYPE_ADD_NEXT, i, arguments);
				if ((res == null) || ((boolean)res == false))
					throw new RuntimeException("Failed to link the node " + i + " to the node " + nextId);
			}
		}

		for (int i=0; i<nbrNode; i++)									// Create chord next
		{
			CommunicationChanel chanel = chanelList.get(i);
			if (chanel == null)
				throw new RuntimeException("Failed to communicate with node " + i);
			int nextChord = neighborhood[nbrNode + i].getFirst();
			if (nextChord < 0) continue;
			LinkedList<Object> arguments = new LinkedList<Object>();
			arguments.add(nextChord);
			Object res = EntryThread.sendActionRequestToNode(chanel, i, Node.MSG_TYPE_CHORD_SET_NEXT, i, arguments);
			if ((res == null) || ((boolean)res == false))
				throw new RuntimeException("Failed to link (chord) the node " + i + " to the node " + nextChord);
		}
	}

// ------------------------------------------
// Local methods
// ------------------------------------------
	public int nbrNode()
	{
		return this.topology.length;
	}

	public Node getNode(int index)
	{
		return this.topology[index];
	}

// ------------------------------------------
// Private methods
// ------------------------------------------
	private static LinkedList<Integer>[] parseTopologyFile(String fileName)
	{
		Scanner sc = null;

		try
		{
			sc = new Scanner(new File(fileName));
			int nbrNode = sc.nextInt();
			@SuppressWarnings("unchecked")
			LinkedList<Integer>[] res = new LinkedList[2*nbrNode];

			for (int y=0; y<nbrNode; y++)							// Overlay
			{
				res[y] = new LinkedList<Integer>();
				for (int x=0; x<nbrNode; x++)
				{
					int n = sc.nextInt();
					if(n == 0) continue;
					res[y].addLast(x);
				}
			}

			for (int y=nbrNode; y<2*nbrNode; y++)					// Chord
			{
				int nextChord = sc.nextInt();
				res[y] = new LinkedList<Integer>();
				res[y].add(nextChord);
			}
			sc.close();
			return res;
		}
		catch (InputMismatchException e)
		{
			throw new RuntimeException("Unknown char in the neighborhood matrix file");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Corrupted neighborhood file");
		}
	}

}