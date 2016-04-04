package p2p;

import general.IpAddress;
import general.Serialization_string;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedList;
import communication.CommunicationChanel;
import communication.ExceptionUnknownCommunicationChanelType;
import communication.Logger;
import communication.RootingTable;







public class Node
{
// ---------------------------------
// Attributes
// ---------------------------------
	public static final int		maxNbrNodes				= 160;// TODO (int) Math.pow(2, 160);
	public static final int		maxNbrMsgHopes			= 5;

	// May be executed by user out of the node
	public static final 	String	MSG_TYPE_SIMPLE_MSG		= "simpleMsg";
	public static final 	String	MSG_TYPE_ADD_NEXT		= "addNext";
/*	public static final 	String	MSG_TYPE_GET_PREVIOUS	= "getPrevious";
	public static final 	String	MSG_TYPE_GET_NEXT		= "getNext";
	public static final 	String	MSG_TYPE_INSERT			= "insert";
	public static final 	String	MSG_TYPE_IS_RESPONSIBLE_FOR_KEY = "isResponsibleForKey";
	public static final 	String	MSG_TYPE_GET_VALUE		= "getValue";
	public static final		String	MSG_TYPE_JOIN			= "join";
*/
	// May be executed by user out of the node
	private static final	String	MSG_TYPE_SET_PREVIOUS	= "setPrevious";

	private int								nodeId;
	private int								previousId;
	private LinkedList<Integer>				nextIdList;
	private LinkedList<CommunicationChanel>	nextChanelList;
	private NodeInternalHashTable			internalHashTable;
	private Logger							logger;
	private String							communicationChanelType;
	private LinkedList<String>				receivedMsgIdList;
	private RootingTable					rootingTable;		// Maps to each known node id a list of successor that have a root to it
																// The list are sorted using the proba to reach the dest and the speed to reach it
// ---------------------------------
// Builder
// ---------------------------------
	public Node(int nodeId, String communicationChanelType) throws ExceptionUnknownCommunicationChanelType
	{
		this.nodeId						= nodeId; //TODO NodeInternalHashTable.hash(""+nodeId);
		this.previousId					= -1;
		this.nextIdList					= new LinkedList<Integer>();
		this.nextChanelList				= new LinkedList<CommunicationChanel>();
		this.logger						= new Logger(""+nodeId);
		this.communicationChanelType	= new String(communicationChanelType);
		this.receivedMsgIdList			= new LinkedList<String>();
		this.rootingTable				= new RootingTable();

		EntryThread.initEntryThread(this, nodeId, logger, communicationChanelType);
	}

// ---------------------------------
// Getter
// ---------------------------------
	public int getNodeId()
	{
		return this.nodeId;
	}

	public LinkedList<Integer> getNext()
	{
		return new LinkedList<Integer>(this.nextIdList);
	}

	public int getPrevious()
	{
		return this.previousId;
	}

	public LinkedList<String> getKeySet()
	{
		if (this.internalHashTable == null)
			return null;

		return this.internalHashTable.getKeySet();
	}

	public String getValue(String key)
	{
		if (this.internalHashTable != null)
		{
			return this.internalHashTable.getValue(key);
		}
		return null;
	}

// ---------------------------------
// Ring overlay management
// ---------------------------------
	public String retransmitMsg(String msgType, int destId, String msgId, int nbrHope, LinkedList<String> argTab)
	{
		LinkedList<Integer> optimizedNextList;
		int newNbrHope = nbrHope-1;

		if (destId == this.nodeId)
			return this.processMsg(msgId, msgType, argTab);

		if (this.nextChanelList.isEmpty())													// Case: node has no next
			return null;

		if ((newNbrHope < 0) && (!this.nextIdList.contains(destId)))						// Case: the massage has exceeded the number of retransmissions
			return null;

		optimizedNextList = rootingTable.getSortedRootToDestination(destId, nextIdList);	// List of the successor nodes sorted using
																							//		the proba that they may reach the destination
		for (int nextId: optimizedNextList)
		{
			int nextIndex = -1;
			for (int i=0; i<nextIdList.size(); i++)
				if (nextId == nextIdList.get(i))
				{
					nextIndex = i;
					break;
				}
			if (nextIndex == -1) throw new RuntimeException();
			CommunicationChanel chanel = this.nextChanelList.get(nextIndex);
			boolean test = true;
			test &= chanel.writeLine(msgType);
			test &= chanel.writeLine(""+destId);
			test &= chanel.writeLine(msgId);
			test &= chanel.writeLine(""+newNbrHope);
			test &= chanel.writeLine(""+argTab.size());
			for (String arg: argTab)
				test &= chanel.writeLine(arg.toString());

			String res = chanel.readLine();
			if ((!test) || (res == null))
			{
				this.rootingTable.decreaseProba(destId, nextId);							// Update the rooting table by decreasing the proba to reach
				continue;																	//		 destId through nextId
			}
			this.rootingTable.addRoot(destId, nextId);										// Update the rooting table by adding the next
			this.logger.write("- Transmit message \"" + msgType+ "\" to \""+destId+"\"\n");	//		in the rooting list of the destination
			return res;
		}
		this.logger.write("- **** Failed to retransmit message \"" + msgType+ "\" to \"" + destId+ "\" ****\n");
		return null;
	}

	private String processMsg(String msgId, String msgType, LinkedList<String> argStr)
	{
		LinkedList<Object> argObj = Serialization_string.getObjectTabFromSerializedStringTab(argStr);

		this.logger.write("- \"" + msgType + "\n");
		this.logger.write("\"\t->\"" + msgId + "\"\n");
		for (Object arg: argObj)
			this.logger.write("\"\t->\"" + arg + "\"\n");			

		if (this.receivedMsgIdList.contains(msgId))
		{
			this.logger.write(" already received\n");
			return null;
		}

		this.logger.write("\n");
		try
		{
			Method	m	= Node.class.getMethod(msgType, argObj.getClass());
			Object	res	= m.invoke(this, argObj);
			this.logger.write("\t-> " + res + "\"\n\n\n");
			return Serialization_string.getSerializedStringFromObject((Serializable) res);
		}
		catch(NoSuchMethodException e)
		{
			e.printStackTrace();
			this.logger.write("\t- **** Unknown message type: \"" + msgType + "\"");
			return null;
		}
		catch(ExceptionWrongRequestArgument e)
		{
			e.printStackTrace();
			this.logger.write("\t- **** Wrong argument type for action: \"" + msgType + "\"");
			return null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			this.logger.write("\t- **** Failed to execute: \"" + msgType + "\". Error: \"" + e + "\"");
			return null;
		}
	}

// ------------------------------------------
// Management methods
// ------------------------------------------
	public boolean simpleMsg(LinkedList<Object> arguments)
	{
		this.logger.write("\t\"" + arguments.get(0) + "\"\n");
		return true;
	}

	public boolean addNext(LinkedList<Object> arguments)
	{
		int		nextId = -1;
		String	nextIP = null;

		try
		{
			nextId = (int)arguments.get(0);
			nextIP = (String) arguments.get(1);
			if (!IpAddress.validIP(nextIP))
				throw new ExceptionWrongRequestArgument();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		if (nextId < 0)
			return false;
		if (this.nextIdList.contains(nextId))
			return true;

		CommunicationChanel chanel = EntryThread.connectToNode(true, communicationChanelType, nodeId, nextId, nextIP);
		if (chanel == null)
		{
			logger.write("\t**** Fail ****\n");
			return false;
		}

		Object[] newArguments = new Object[1];
		newArguments[0] = this.nodeId;
		Boolean res = (Boolean)EntryThread.sendActionRequestToNode(chanel, nodeId, MSG_TYPE_SET_PREVIOUS, nextId, newArguments);
		if ((res == null) || (res == false))
		{
			logger.write("\t**** Fail ****\n");
			return false;
		}
		this.nextIdList.addLast(nextId);
		this.nextChanelList.addLast(chanel);
		logger.write("\tnext added = " + nextId + "\n");

		return true;
	}

	public boolean setPrevious(LinkedList<Object> arguments)
	{
		int previousId = -1;

		try
		{
			previousId = (int) arguments.get(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		if (previousId < 0)
			return false;
//TODO change this by un update of the existing internal hash table
		if (previousId != this.previousId)
			this.internalHashTable	= new NodeInternalHashTable(previousId, nodeId);
		this.previousId = previousId;
		return true;
	}

/*
// -----------------------------------------------
// Chord algorithm
// -----------------------------------------------
TODO
	private boolean insert(String key, String value)
	{
		boolean test;
		
		if (this.internalHashTable != null)
		{
			test = this.internalHashTable.insert(key, value);
			if (test)
				return true;
		}

		if (this.nextChanel == null)
			return false;

		test = true;
		test &= this.nextChanel.writeLine(Node.CHORD_ACTIONINSERT);
		test &= this.nextChanel.writeLine(key);
		test &= this.nextChanel.writeLine(value);
		return test;
	}

	private Boolean isResponsibleForKey(String key)
	{
		if (this.internalHashTable == null)
			return null;

		return this.internalHashTable.isResponsibleForKey(key);
	}

*/
}