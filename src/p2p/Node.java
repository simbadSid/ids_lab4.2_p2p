package p2p;

import general.General;
import general.Serialization_string;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Calendar;
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
	public static final 	String	MSG_TYPE_GET_PREVIOUS	= "getPrevious";
	public static final 	String	MSG_TYPE_GET_NEXT		= "getNext";

	public static final 	String	MSG_TYPE_CHORD_SET_NEXT	= "chord_setNext";
	public static final 	String	MSG_TYPE_CHORD_IS_RESPONSIBLE_FOR_KEY = "chord_isResponsibleForKey";
	public static final 	String	MSG_TYPE_CHORD_GET_RESPONSIBLE_FOR_KEY = "chord_getResponsibleForKey";
	public static final 	String	MSG_TYPE_CHORD_INSERT	= "chord_insert";
/*	public static final		String	MSG_TYPE_CHORD_JOIN		= "chord_join";
	public static final 	String	MSG_TYPE_CHORD_GET_VALUE= "chord_getValue";
*/
	// May be executed by nodes only
	private static final	String	MSG_TYPE_SET_PREVIOUS		= "setPrevious";
	private static final 	String	MSG_TYPE_CHORD_SET_PREVIOUS	= "chord_setPrevious";

	private int								nodeId;
	private int								previousId;
	private int								nextChord;
	private int								previousChord;
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
		this.nextChord					= -1;
		this.previousChord				= -1;
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

	public int getChordNext()
	{
		return this.nextChord;
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
	public String retransmitMsg(RequestPacket request, boolean allowDoubles)
	{
System.out.println("retransmit");
System.out.println("Local= " + this.nodeId);
System.out.println("Dest = " + request.destNodeId);
		LinkedList<Integer> optimizedNextList;
		request.nbrHope --;

		if ((this.receivedMsgIdList.contains(request.msgId)) && (!allowDoubles))
		{
			this.logger.write(" already received\n");
			return null;
		}
		this.receivedMsgIdList.addLast(request.msgId);

		if (request.destNodeId == this.nodeId)
			return this.processMsg(request.msgId, request.msgType, request.arguments);

		if (this.nextChanelList.isEmpty())													// Case: node has no next
			return null;

		if ((request.nbrHope < 0) && (!this.nextIdList.contains(request.destNodeId)))		// Case: the massage has exceeded the number of retransmissions
			return null;

		optimizedNextList = rootingTable.getSortedRootToDestination(request.destNodeId, nextIdList);// List of the successor nodes sorted using
																							//		the proba that they may reach the destination
		for (int nextIndex: optimizedNextList)
		{
			CommunicationChanel chanel		= this.nextChanelList.get(nextIndex);
			int					nextId		= this.nextIdList.get(nextIndex);
			String				requestStr	= Serialization_string.getSerializedStringFromObject(request);
System.out.println("\t Waiting for " + nextId);
			boolean test = chanel.writeLine(requestStr);

			String res		= chanel.readLine();
			Object resObj	= Serialization_string.getObjectFromSerializedString(res);
System.out.println("\t Response from " + nextId + " : " + resObj);
			if ((!test) || (resObj == null))
			{
				this.rootingTable.decreaseProba(request.destNodeId, nextId);				// Update the rooting table by decreasing the proba to reach
				continue;																	//		 destId through nextId
			}
			this.rootingTable.addRoot(request.destNodeId, nextId);							// Update the rooting table by adding the next
																							//		in the rooting list of the destination
			this.logger.write("- Transmit message \"" + request.msgType+ "\" to \""+request.destNodeId+"\"\n");
			return res;
		}
		this.logger.write("- **** Failed to retransmit message \"" + request.msgType+ "\" to \"" + request.destNodeId+ "\" ****\n");
		return null;
	}

	private String processMsg(String msgId, String msgType, LinkedList<Object> argObj)
	{
		this.logger.write("- \"" + msgType + "\n");
		this.logger.write("\"\t->\"" + msgId + "\"\n");
		if (argObj != null)
			for (Object arg: argObj)
				this.logger.write("\"\t->\"" + arg + "\"\n");
		else
			this.logger.write("\"\t->\"" + "(no arguments)" + "\"\n");
		this.logger.write("\n");
		try
		{
			Method	m	= Node.class.getMethod(msgType, LinkedList.class, String.class);
			Object	res	= m.invoke(this, argObj, msgId);
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
	public boolean simpleMsg(LinkedList<Object> arguments, String msgId)
	{
		this.logger.write("\t\"" + arguments.get(0) + "\"\n");
		return true;
	}

	public boolean addNext(LinkedList<Object> arguments, String msgId)
	{
		int		nextId = -1;
		String	nextIP = null;

		try
		{
			nextId = (int)arguments.get(0);
			nextIP = (String) arguments.get(1);
			if ((nextId < 0) || (!General.validIP(nextIP)))
				throw new ExceptionWrongRequestArgument();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		if (this.nextIdList.contains(nextId))
			return true;

		CommunicationChanel chanel = EntryThread.connectToNode(true, communicationChanelType, nodeId, nextId, nextIP);
		if (chanel == null)
		{
			logger.write("\t**** Fail ****\n");
			return false;
		}

		LinkedList<Object> newArguments = new LinkedList<Object>();
		newArguments.add(this.nodeId);
//TODO change by a call to this.transmit
		Boolean res = (Boolean) EntryThread.sendActionRequestToNode(chanel, nodeId, MSG_TYPE_SET_PREVIOUS, nextId, newArguments);
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

	public boolean setPrevious(LinkedList<Object> arguments, String msgId)
	{
		int previousId = -1;

		try
		{
			previousId = (int) arguments.get(0);
			if (previousId < 0) throw new ExceptionWrongRequestArgument();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		if (previousId < 0)
			return false;
		this.previousId = previousId;
		return true;
	}


	public LinkedList<Integer> getNext(LinkedList<Object> arguments, String msgId)
	{
		return this.getNext();
	}

	public int getPrevious(LinkedList<Object> arguments, String msgId)
	{
		return this.getPrevious();
	}

// -----------------------------------------------
// Chord algorithm
// -----------------------------------------------
	public Boolean chord_setNext(LinkedList<Object> arguments, String msgId)
	{
		int nextChord;

		try
		{
			nextChord = (int) arguments.get(0);
			if (nextChord < 0) throw new ExceptionWrongRequestArgument();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		String	newMsgId = "" + this.nodeId + Calendar.getInstance().getTime() + System.nanoTime();
		LinkedList<Object> newArguments = new LinkedList<Object>();
		newArguments.add(this.nodeId);
		RequestPacket request = new RequestPacket(MSG_TYPE_CHORD_SET_PREVIOUS, nextChord, newMsgId, maxNbrMsgHopes, newArguments);
		String res = this.retransmitMsg(request, false);
		Boolean test = (Boolean) Serialization_string.getObjectFromSerializedString(res);
		if ((test == null) || (test == false))
			return false;

		this.nextChord = nextChord;
		return true;
	}

	public Boolean chord_setPrevious(LinkedList<Object> arguments, String msgId)
	{
		int previousChord;

		try
		{
			previousChord = (int) arguments.get(0);
			if (previousChord < 0) throw new ExceptionWrongRequestArgument();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new ExceptionWrongRequestArgument();
		}

		//TODO change this by an update of the existing internal hash table
		if (previousChord != this.previousChord)
			this.internalHashTable	= new NodeInternalHashTable(previousId, nodeId);

		return true;
	}

	public Boolean chord_isResponsibleForKey(LinkedList<Object> arguments, String msgId)
	{
		String key = (String) arguments.get(0);

		if (this.internalHashTable == null)
			return false;

		return this.internalHashTable.isResponsibleForKey(key);
	}

	public Integer chord_getResponsibleForKey(LinkedList<Object> arguments, String msgId)
	{
		String key = (String) arguments.get(0);

		if ((this.internalHashTable != null) && (this.internalHashTable.isResponsibleForKey(key)))
			return this.nodeId;

		if (this.nextChord < 0)
			return null;

		LinkedList<Object> newArguments = new LinkedList<Object>();
		newArguments.add(key);
		RequestPacket request = new RequestPacket(MSG_TYPE_CHORD_GET_RESPONSIBLE_FOR_KEY, nextChord, msgId, maxNbrMsgHopes, newArguments);
		String res = this.retransmitMsg(request, true);
		return (Integer) Serialization_string.getObjectFromSerializedString(res);
	}

//TODO
	public Boolean chord_insert(LinkedList<Object> arguments, String msgId)
	{
		String key	= (String) arguments.get(0);
		String value= (String) arguments.get(0);
		boolean test;

		if (this.internalHashTable != null)
		{
			test = this.internalHashTable.insert(key, value);
			if (test)
				return true;
		}

		LinkedList<Object> newArguments = new LinkedList<Object>();
		newArguments.add(key);
		RequestPacket request = new RequestPacket(MSG_TYPE_CHORD_GET_RESPONSIBLE_FOR_KEY, nextChord, msgId, maxNbrMsgHopes, newArguments);
		String res = this.retransmitMsg(request, true);
		return (Boolean) Serialization_string.getObjectFromSerializedString(res);

		
		if (responsible < 0)
			return false;

		String	msgId = "" + this.nodeId + Calendar.getInstance().getTime() + System.nanoTime();
		RequestPacket request = new RequestPacket(MSG_TYPE_CHORD_INSERT, nextChord, msgId, maxNbrMsgHopes, arguments);
		String res = this.retransmitMsg(request);
		return (Boolean) Serialization_string.getObjectFromSerializedString(res);
	}

//TODO
	public Boolean chord_join(LinkedList<Object> arguments, String msgId)
	{
		
	}

	public Object chord_getValue(LinkedList<Object> arguments, String msgId)
	{
		
	}
}