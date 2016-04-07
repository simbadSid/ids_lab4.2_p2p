package p2p;

import general.Serialization_string;
import general.SynchronizedList;
import java.util.LinkedList;
import communication.CommunicationChanel;
import communication.Logger;






public class EntryThread implements Runnable
{
// -----------------------------------
// Attributes
// -----------------------------------
	private static final int	initialEntryPort			= 2222;
	private static final String	chanelName_login_input		= "EntryThreadChanel_input_";
	private static final String	chanelName_login_output		= "EntryThreadChanel_output_";
	private static final String	chanelName_executer_input	= "EntryThreadChanel_executer_input_";
	private static final String	chanelName_executer_output	= "EntryThreadChanel_executer_output_";
	private static final String	logingAnswer_accept			= "connectionAccepted";

	private Node						node;
	private int							nodeId;
	private int							nodePort;
	private Logger						logger;
	private String						communicationChanelType;
	//TODO	private Object				lock = new Object();
	private SynchronizedList<Integer>	connectedNodes;

// -----------------------------------
// Builder
// -----------------------------------
	public static void initEntryThread(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		new EntryThread(node, nodeId, logger, communicationChanelType);
	}

	private EntryThread(Node node, int nodeId, Logger logger, String communicationChanelType)
	{
		this.node						= node;
		this.nodeId						= nodeId;
		this.nodePort					= getPort(nodeId);
		this.logger						= logger;
		this.communicationChanelType	= communicationChanelType;
		this.connectedNodes				= new SynchronizedList<Integer>();

		Thread t = new Thread(this);
		t.start();
	}

// -----------------------------------
// Connection Thread
// -----------------------------------
	@Override
	public void run()
	{
		CommunicationChanel chanel = CommunicationChanel.instantiate(communicationChanelType, null, -1, nodePort, true, true, getLoginChanelOutputName(nodeId), getLoginChanelInputName(nodeId));

		if (chanel == null)
		{
			logger.write("**** Entry thread failed to initialize ****\n");
			return;
		}

		logger.write("Entry thread: starts waiting\n");
		Integer callerNodeId;
		while((callerNodeId = chanel.readInt()) != null)
		{
			boolean test = chanel.writeLine(logingAnswer_accept);
			CommunicationChanel executerChanel = CommunicationChanel.instantiate(communicationChanelType, null, -1, nodePort, true, true, getExecuterChanelOutputName(nodeId, callerNodeId), getExecuterChanelInputName(nodeId, callerNodeId));
			if ((!test) || (executerChanel == null))
			{
				logger.write("**** Entry thread failed to answer to logged user: " + callerNodeId + "****\n");
				continue;
			}

			if (connectedNodes.contains(callerNodeId))
				continue;
			connectedNodes.addLast(callerNodeId);
			logger.write("Entry thread: successful login to " + callerNodeId + "\n\n\n");
			Thread t = new Thread(new ExecuterHead(executerChanel, callerNodeId));
			t.start();
		}
	}

// -----------------------------------
// Executer Thread
// -----------------------------------
private class ExecuterHead implements Runnable
{
	// Attributes
	CommunicationChanel chanel;
	int					remoteNodeId;

	// Builder
	public ExecuterHead (CommunicationChanel chanel, int remoteNodeId)
	{
		this.chanel			= chanel;
		this.remoteNodeId	= remoteNodeId;
	}

	@Override
	public void run()
	{
		String reauestStr;

		while((reauestStr = chanel.readLine()) != null)
		{
			RequestPacket	request 	= (RequestPacket) Serialization_string.getObjectFromSerializedString(reauestStr);
			Executer		executer	= new Executer(request, chanel);
			new Thread(executer).start();
		}

		this.chanel.close();
		connectedNodes.remove(this.remoteNodeId);
	}
}

private class Executer implements Runnable
{
	// Attributes
	RequestPacket		request;
	CommunicationChanel	chanel;

	// Builder
	public Executer(RequestPacket request, CommunicationChanel chanel)
	{
		this.request	= request;
		this.chanel		= chanel;
	}

	// Local methods
	@Override
	public void run()
	{
		String res = null;
		
		try
		{
//TODO			synchronized(lock)
//			{
				res = node.retransmitMsg(request);
//			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
//			System.exit(0);
		}
		chanel.writeLine(res);
	}
}
// -----------------------------------
// Snippet of code that can be executed by 
// 	a foreign user (on his local machine)
// -----------------------------------
	public static CommunicationChanel connectToNode(boolean isNode, String chanelType, int localNodeId, int remoteNodeId, String remoteNodeIP)
	{
		int newLocalNodeId = localNodeId;

		if (!isNode)
			newLocalNodeId = -newLocalNodeId - 1;
		String	writerChanelName= EntryThread.getLoginChanelInputName(remoteNodeId);
		String	readerChanelName= EntryThread.getLoginChanelOutputName(remoteNodeId);
		int		remoteNodePort	= EntryThread.getPort(remoteNodeId);

		CommunicationChanel chanel = CommunicationChanel.instantiate(chanelType, remoteNodeIP, remoteNodePort, -1, true, true, writerChanelName, readerChanelName);

		if (chanel == null)
			return null;

		boolean test = chanel.writeLine(""+newLocalNodeId);
		if (!test)
		{
			chanel.close();
			return null;
		}

		String answer = chanel.readLine();
		chanel.close();
		if ((answer == null) || (!answer.equals(logingAnswer_accept)))
		{
			return null;
		}

		writerChanelName= EntryThread.getExecuterChanelInputName(remoteNodeId, newLocalNodeId);
		readerChanelName= EntryThread.getExecuterChanelOutputName(remoteNodeId, newLocalNodeId);
		chanel = CommunicationChanel.instantiate(chanelType, remoteNodeIP, remoteNodePort, -1, true, true, writerChanelName, readerChanelName);

		return chanel;
	}
	/**
 	 * Used by a user (not a node) to send a request to a node.
	 * @param chanelType
	 * @param inputNodeId: id of the node which will communicate with the user
	 * @param inputNodeIP: IP of the node which will communicate with the user
	 * @param destNodeId: id of the node that will receive the message from inputNodeId
	 * @param msgType: Node.MSG_TYPE_....
	 * @param arguments: list of the arguments of the request
	 * @return
	 */
	public static Object sendActionRequestToNode(CommunicationChanel chanel, int inputNodeId, String msgType, int destNodeId, LinkedList<Object> arguments)
	{
		String	msgId = Node.msgId(inputNodeId);
		RequestPacket request = new RequestPacket(msgType, destNodeId, msgId, Node.maxNbrMsgHopes, arguments);
		String requestStr = Serialization_string.getSerializedStringFromObject(request);

		boolean test = chanel.writeLine(requestStr);
		if (test == false)
		{
			throw new RuntimeException("Failed while sending the request to the node: " + inputNodeId + "\n");
		}

		String res = chanel.readLine();
		return Serialization_string.getObjectFromSerializedString(res);
	}

// ---------------------------------------------------
// Local private method
// ---------------------------------------------------
	private static String getLoginChanelInputName(int entryThreadNodeId)
	{
		return EntryThread.chanelName_login_input + entryThreadNodeId;
	}

	private static String getLoginChanelOutputName(int entryThreadNodeId)
	{
		return EntryThread.chanelName_login_output + entryThreadNodeId;
	}

	private static String getExecuterChanelInputName(int entryThreadNodeId, int callerNodeId)
	{
		return EntryThread.chanelName_executer_input + entryThreadNodeId + "_" + callerNodeId;
	}

	private static String getExecuterChanelOutputName(int entryThreadNodeId, int callerNodeId)
	{
		return EntryThread.chanelName_executer_output + entryThreadNodeId + "_" + callerNodeId;
	}

	private static int getPort(int nodeId)
	{
		return initialEntryPort + nodeId;
	}
}